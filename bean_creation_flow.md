# 프록시 로그 추적 시스템 빈 생성 및 주입 순서 분석

## 개요
Spring Boot 애플리케이션 시작 시 InterfaceProxyConfig를 통한 빈 생성과 의존성 주입이 어떤 순서로 이루어지는지 상세히 분석합니다.

---

## Spring 컨테이너 초기화 과정

### 애플리케이션 시작 단계
```
Spring Boot Application 시작
    ↓
Component Scanning 및 Configuration 클래스 탐지
    ↓
Bean Definition 등록
    ↓
Bean 생성 및 의존성 주입 (순환 참조 해결)
    ↓
애플리케이션 컨텍스트 완성
```

---

## 1단계: Bean Definition 등록 (애플리케이션 시작 시)

### 1.1 Component Scanning
```java
// 자동으로 스캔되는 컴포넌트들
@RestController
public class OrderControllerV1ApiAdapter { ... }  // 스캔 대상

// 스캔에서 제외되는 구현체들 (수동 빈 등록을 위해)
// OrderControllerV1Impl - 컴포넌트 스캔 제외
// OrderServiceV1Impl - 컴포넌트 스캔 제외  
// OrderRepositoryV1Impl - 컴포넌트 스캔 제외
```

### 1.2 Configuration 클래스 등록
```java
@Configuration
public class InterfaceProxyConfig {
    // Bean Definition들이 Spring 컨테이너에 등록됨
    @Bean orderController(LogTrace logTrace) { ... }
    @Bean orderService(LogTrace logTrace) { ... }
    @Bean orderRepository(LogTrace logTrace) { ... }
}
```

---

## 2단계: Bean 생성 순서 (의존성 그래프 기준)

### 2.1 의존성 그래프 분석
```
OrderControllerV1ApiAdapter
    ↓ (의존)
OrderControllerV1 (프록시)
    ↓ (의존)
OrderServiceV1 (프록시)
    ↓ (의존)
OrderRepositoryV1 (프록시)
    ↓ (의존)
LogTrace (ThreadLocalLogTrace)
```

### 2.2 실제 생성 순서 (Bottom-Up)

#### **1순위: ThreadLocalLogTrace 빈 생성**
```java
// 가장 먼저 생성 (다른 빈들의 의존성이므로)
@Component
public class ThreadLocalLogTrace implements LogTrace {
    private ThreadLocal<TraceId> traceIdHolder = new ThreadLocal<>();
    
    // 생성자 실행
    public ThreadLocalLogTrace() {
        System.out.println("ThreadLocalLogTrace 생성됨");
    }
}
```
- **생성 이유**: 다른 모든 프록시 빈들이 이 빈에 의존
- **생성 시점**: 가장 우선순위로 생성
- **빈 이름**: `threadLocalLogTrace`

#### **2순위: OrderRepositoryV1 프록시 빈 생성**
```java
@Configuration
public class InterfaceProxyConfig {
    
    @Bean
    public OrderRepositoryV1 orderRepository(LogTrace logTrace) {
        System.out.println("OrderRepository 프록시 생성 시작");
        
        // 1. 실제 구현체 생성
        OrderRepositoryV1 orderRepositoryV1 = new OrderRepositoryV1Impl();
        System.out.println("OrderRepositoryV1Impl 생성됨");
        
        // 2. 프록시 생성 (구현체 + 로그추적기 조합)
        OrderRepositoryInterfaceProxy proxy = 
            new OrderRepositoryInterfaceProxy(orderRepositoryV1, logTrace);
        System.out.println("OrderRepositoryInterfaceProxy 생성됨");
        
        return proxy; // 프록시 객체를 빈으로 등록
    }
}
```
- **의존성**: `LogTrace logTrace` (이미 생성됨)
- **생성 과정**:
  1. `OrderRepositoryV1Impl` 인스턴스 생성
  2. `OrderRepositoryInterfaceProxy` 생성 (구현체 + 로그추적기 래핑)
- **빈 이름**: `orderRepository`
- **실제 타입**: `OrderRepositoryInterfaceProxy`

#### **3순위: OrderServiceV1 프록시 빈 생성**
```java
@Bean
public OrderServiceV1 orderService(LogTrace logTrace) {
    System.out.println("OrderService 프록시 생성 시작");
    
    // 1. 실제 구현체 생성 (Repository 의존성 주입)
    OrderServiceV1 orderServiceV1 = 
        new OrderServiceV1Impl(orderRepository(logTrace)); // Repository 프록시 주입
    System.out.println("OrderServiceV1Impl 생성됨 (Repository 프록시 주입됨)");
    
    // 2. 프록시 생성
    OrderServiceInterfaceProxy proxy = 
        new OrderServiceInterfaceProxy(orderServiceV1, logTrace);
    System.out.println("OrderServiceInterfaceProxy 생성됨");
    
    return proxy;
}
```
- **의존성**: 
  - `LogTrace logTrace` (이미 생성됨)
  - `orderRepository()` 메서드 호출 → Repository 프록시 빈 참조
- **생성 과정**:
  1. `orderRepository()` 호출하여 Repository 프록시 획득
  2. Repository 프록시를 주입받은 `OrderServiceV1Impl` 생성
  3. `OrderServiceInterfaceProxy` 생성
- **빈 이름**: `orderService`

#### **4순위: OrderControllerV1 프록시 빈 생성**
```java
@Bean
public OrderControllerV1 orderController(LogTrace logTrace) {
    System.out.println("OrderController 프록시 생성 시작");
    
    // 1. 실제 구현체 생성 (Service 의존성 주입)
    OrderControllerV1 orderControllerV1 = 
        new OrderControllerV1Impl(orderService(logTrace)); // Service 프록시 주입
    System.out.println("OrderControllerV1Impl 생성됨 (Service 프록시 주입됨)");
    
    // 2. 프록시 생성
    OrderControllerInterfaceProxy proxy = 
        new OrderControllerInterfaceProxy(orderControllerV1, logTrace);
    System.out.println("OrderControllerInterfaceProxy 생성됨");
    
    return proxy;
}
```
- **의존성**:
  - `LogTrace logTrace` (이미 생성됨)
  - `orderService()` 메서드 호출 → Service 프록시 빈 참조
- **생성 과정**:
  1. `orderService()` 호출하여 Service 프록시 획득
  2. Service 프록시를 주입받은 `OrderControllerV1Impl` 생성
  3. `OrderControllerInterfaceProxy` 생성
- **빈 이름**: `orderController`

#### **5순위: OrderControllerV1ApiAdapter 빈 생성**
```java
@RestController
public class OrderControllerV1ApiAdapter {
    
    private final OrderControllerV1 orderController;
    
    // 생성자 주입
    public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
        System.out.println("OrderControllerV1ApiAdapter 생성 시작");
        System.out.println("주입받은 orderController: " + orderController.getClass());
        // 실제로는: OrderControllerInterfaceProxy 인스턴스가 주입됨
        
        this.orderController = orderController;
        System.out.println("OrderControllerV1ApiAdapter 생성 완료");
    }
}
```
- **의존성**: `OrderControllerV1 orderController` (프록시 빈)
- **생성 과정**:
  1. Spring이 `OrderControllerV1` 타입의 빈 검색
  2. `InterfaceProxyConfig.orderController()` 빈 발견
  3. 해당 빈(실제로는 프록시 인스턴스)을 생성자에 주입
- **빈 이름**: `orderControllerV1ApiAdapter`

---

## 3단계: 빈 생성 상세 흐름

### 3.1 OrderRepository 프록시 생성 상세
```java
// InterfaceProxyConfig.orderRepository() 메서드 실행
public OrderRepositoryV1 orderRepository(LogTrace logTrace) {
    // Step 1: 실제 구현체 인스턴스 생성
    OrderRepositoryV1Impl impl = new OrderRepositoryV1Impl();
    
    // Step 2: 프록시 인스턴스 생성
    OrderRepositoryInterfaceProxy proxy = new OrderRepositoryInterfaceProxy(impl, logTrace);
    
    // Step 3: 프록시 내부 구조
    // proxy.target = impl (실제 구현체 참조)
    // proxy.logTrace = logTrace (로그 추적기 참조)
    
    return proxy; // 프록시를 빈으로 등록
}
```

### 3.2 OrderService 프록시 생성 상세
```java
public OrderServiceV1 orderService(LogTrace logTrace) {
    // Step 1: Repository 프록시 빈 획득
    OrderRepositoryV1 repositoryProxy = orderRepository(logTrace);
    // ↑ 이미 생성된 빈이므로 기존 인스턴스 반환
    
    // Step 2: Service 구현체 생성 (Repository 프록시 주입)
    OrderServiceV1Impl impl = new OrderServiceV1Impl(repositoryProxy);
    
    // Step 3: Service 프록시 생성
    OrderServiceInterfaceProxy proxy = new OrderServiceInterfaceProxy(impl, logTrace);
    
    // Step 4: 프록시 내부 구조
    // proxy.target = impl
    // impl.orderRepository = repositoryProxy (Repository 프록시)
    
    return proxy;
}
```

### 3.3 OrderController 프록시 생성 상세
```java
public OrderControllerV1 orderController(LogTrace logTrace) {
    // Step 1: Service 프록시 빈 획득
    OrderServiceV1 serviceProxy = orderService(logTrace);
    // ↑ 이미 생성된 빈이므로 기존 인스턴스 반환
    
    // Step 2: Controller 구현체 생성 (Service 프록시 주입)
    OrderControllerV1Impl impl = new OrderControllerV1Impl(serviceProxy);
    
    // Step 3: Controller 프록시 생성
    OrderControllerInterfaceProxy proxy = new OrderControllerInterfaceProxy(impl, logTrace);
    
    // Step 4: 프록시 내부 구조
    // proxy.target = impl
    // impl.orderService = serviceProxy (Service 프록시)
    
    return proxy;
}
```

---

## 4단계: 최종 빈 컨테이너 상태

### 4.1 등록된 빈 목록
```
Spring 컨테이너에 등록된 빈들:

1. threadLocalLogTrace
   - 타입: ThreadLocalLogTrace
   - 실제 인스턴스: ThreadLocalLogTrace@12345

2. orderRepository  
   - 타입: OrderRepositoryV1 (인터페이스)
   - 실제 인스턴스: OrderRepositoryInterfaceProxy@67890
   
3. orderService
   - 타입: OrderServiceV1 (인터페이스)  
   - 실제 인스턴스: OrderServiceInterfaceProxy@11111

4. orderController
   - 타입: OrderControllerV1 (인터페이스)
   - 실제 인스턴스: OrderControllerInterfaceProxy@22222

5. orderControllerV1ApiAdapter
   - 타입: OrderControllerV1ApiAdapter
   - 실제 인스턴스: OrderControllerV1ApiAdapter@33333
```

### 4.2 객체 참조 관계도
```
OrderControllerV1ApiAdapter@33333
  ↓ orderController 필드
OrderControllerInterfaceProxy@22222
  ├── target: OrderControllerV1Impl@44444
  └── logTrace: ThreadLocalLogTrace@12345
          ↓ orderService 필드  
      OrderServiceInterfaceProxy@11111
        ├── target: OrderServiceV1Impl@55555
        └── logTrace: ThreadLocalLogTrace@12345 (동일 인스턴스)
                ↓ orderRepository 필드
            OrderRepositoryInterfaceProxy@67890
              ├── target: OrderRepositoryV1Impl@66666
              └── logTrace: ThreadLocalLogTrace@12345 (동일 인스턴스)
```

---

## 5단계: 의존성 주입 검증

### 5.1 주입 검증 과정
```java
// API 어댑터에서 실제 주입된 객체 확인
@RestController
public class OrderControllerV1ApiAdapter {
    
    public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
        // 런타임에 실제 타입 확인
        System.out.println("주입된 객체 타입: " + orderController.getClass());
        // 출력: OrderControllerInterfaceProxy
        
        System.out.println("인터페이스 구현 여부: " + 
            (orderController instanceof OrderControllerV1));
        // 출력: true
        
        System.out.println("프록시 객체 여부: " + 
            (orderController instanceof OrderControllerInterfaceProxy));
        // 출력: true
    }
}
```

### 5.2 프록시 투명성 검증
```java
// API 어댑터는 프록시인지 실제 구현체인지 구분할 필요 없음
public String request(@RequestParam("itemId") String itemId) {
    // 인터페이스 메서드 호출 - 프록시가 투명하게 처리
    return orderController.request(itemId);
    
    // 실제 실행 흐름:
    // 1. OrderControllerInterfaceProxy.request() 실행
    // 2. 로그 추적 시작
    // 3. OrderControllerV1Impl.request() 위임 호출
    // 4. 로그 추적 종료
}
```

---

## 6단계: 빈 생성 최적화 및 특징

### 6.1 Spring의 빈 생성 최적화
- **싱글톤 보장**: 각 빈은 하나의 인스턴스만 생성
- **순환 참조 감지**: 의존성 그래프에서 순환 참조 자동 감지
- **Lazy 초기화**: 필요시점까지 빈 생성 지연 가능
- **빈 생명주기 관리**: `@PostConstruct`, `@PreDestroy` 등 생명주기 콜백

### 6.2 프록시 빈의 특징
- **인터페이스 타입으로 등록**: 구현체가 아닌 인터페이스 타입으로 빈 등록
- **투명한 프록시**: 클라이언트는 프록시 존재를 알 필요 없음
- **횡단 관심사 분리**: 로그 추적 로직이 비즈니스 로직과 완전 분리
- **런타임 위임**: 프록시가 실제 구현체에 메서드 호출 위임

---

## 결론

이 프로젝트의 빈 생성 및 주입 과정은 다음과 같은 특징을 보입니다:

1. **의존성 기반 순서**: 의존성이 적은 빈부터 순차적으로 생성
2. **프록시 패턴 적용**: 각 계층마다 프록시로 감싸서 로그 추적 기능 추가
3. **인터페이스 기반 설계**: 구현체가 아닌 인터페이스 타입으로 의존성 주입
4. **투명한 프록시 사용**: 클라이언트 코드 변경 없이 횡단 관심사 적용
5. **Spring DI 활용**: 컨테이너가 자동으로 적절한 빈 인스턴스 주입

이를 통해 깔끔한 로그 추적 시스템을 구축하면서도 기존 비즈니스 로직의 변경 없이 횡단 관심사를 효과적으로 적용했습니다.