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

### 1.0 Spring Boot 애플리케이션 시작점
```java
@SpringBootApplication
public class ProxyApplication {
    
    public static void main(String[] args) {
        System.out.println("=== Spring Boot 애플리케이션 시작 ===");
        
        // Spring Boot 애플리케이션 컨텍스트 생성 및 시작
        SpringApplication.run(ProxyApplication.class, args);
        
        System.out.println("=== 애플리케이션 컨텍스트 초기화 완료 ===");
    }
}
```

**ProxyApplication의 역할:**
- **@SpringBootApplication**: 다음 어노테이션들의 조합
  - `@Configuration`: 설정 클래스임을 명시
  - `@EnableAutoConfiguration`: Spring Boot 자동 설정 활성화
  - `@ComponentScan`: 현재 패키지부터 하위 패키지까지 컴포넌트 스캔
- **애플리케이션 시작점**: `main()` 메서드를 통한 Spring 컨테이너 부트스트랩
- **컨텍스트 생성**: `SpringApplication.run()`을 통한 ApplicationContext 생성

### 1.1 Component Scanning 과정
```java
// @SpringBootApplication의 @ComponentScan으로 인해 자동 스캔되는 클래스들

// 1. 메인 애플리케이션 클래스
@SpringBootApplication  // ← 이것이 스캔의 시작점
public class ProxyApplication { ... }

// 2. 설정 클래스
@Configuration  // ← Component Scanning으로 자동 탐지
public class InterfaceProxyConfig { ... }

// 3. 컨트롤러 클래스  
@RestController  // ← Component Scanning으로 자동 탐지
public class OrderControllerV1ApiAdapter { ... }

// 4. 로그 추적 클래스
@Component  // ← Component Scanning으로 자동 탐지
public class ThreadLocalLogTrace implements LogTrace { ... }
```

**스캔 제외되는 클래스들 (의도적 제외):**
```java
// 이 클래스들은 @Component 계열 어노테이션이 없어서 스캔되지 않음
// InterfaceProxyConfig에서 수동으로 빈 등록할 예정

public class OrderControllerV1Impl { ... }  // 스캔 제외
public class OrderServiceV1Impl { ... }     // 스캔 제외  
public class OrderRepositoryV1Impl { ... }  // 스캔 제외
```

### 1.2 Bean Definition 등록 순서
```
Spring Boot 시작
    ↓
1. ProxyApplication 클래스 로딩 (@SpringBootApplication)
    ↓
2. Component Scanning 시작 (기본 패키지: com.choongang.proxy)
    ↓
3. @Configuration 클래스 탐지 → InterfaceProxyConfig 등록
    ↓
4. @RestController 클래스 탐지 → OrderControllerV1ApiAdapter 등록  
    ↓
5. @Component 클래스 탐지 → ThreadLocalLogTrace 등록
    ↓
6. @Bean 메서드 분석 → InterfaceProxyConfig의 빈 정의들 등록
    ↓
7. Bean Definition Registry 완성
```

### 1.3 Configuration 클래스 상세 등록
```java
@Configuration
public class InterfaceProxyConfig {
    
    // Spring이 이 메서드들을 분석하여 Bean Definition으로 등록
    
    @Bean  // ← Bean Definition: orderRepository
    public OrderRepositoryV1 orderRepository(LogTrace logTrace) { 
        // 의존성: LogTrace logTrace
        return new OrderRepositoryInterfaceProxy(...);
    }
    
    @Bean  // ← Bean Definition: orderService  
    public OrderServiceV1 orderService(LogTrace logTrace) {
        // 의존성: LogTrace logTrace, orderRepository() 호출
        return new OrderServiceInterfaceProxy(...);
    }
    
    @Bean  // ← Bean Definition: orderController
    public OrderControllerV1 orderController(LogTrace logTrace) {
        // 의존성: LogTrace logTrace, orderService() 호출  
        return new OrderControllerInterfaceProxy(...);
    }
}
```

### 1.4 최종 Bean Definition Registry 상태
```
=== Bean Definition Registry ===

1. proxyApplication
   - Type: ProxyApplication
   - Scope: Singleton
   - Source: @SpringBootApplication

2. interfaceProxyConfig  
   - Type: InterfaceProxyConfig
   - Scope: Singleton
   - Source: @Configuration

3. orderControllerV1ApiAdapter
   - Type: OrderControllerV1ApiAdapter  
   - Scope: Singleton
   - Source: @RestController
   - Dependencies: [OrderControllerV1]

4. threadLocalLogTrace
   - Type: ThreadLocalLogTrace
   - Scope: Singleton  
   - Source: @Component

5. orderRepository
   - Type: OrderRepositoryV1
   - Scope: Singleton
   - Source: @Bean method
   - Dependencies: [LogTrace]

6. orderService  
   - Type: OrderServiceV1
   - Scope: Singleton
   - Source: @Bean method
   - Dependencies: [LogTrace]

7. orderController
   - Type: OrderControllerV1  
   - Scope: Singleton
   - Source: @Bean method
   - Dependencies: [LogTrace]
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