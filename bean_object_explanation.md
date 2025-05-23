# Spring 빈 vs 일반 객체 관계 상세 설명

## 🎯 핵심 포인트

### 🏛️ **Spring Container가 관리하는 빈들**
Spring이 생명주기를 관리하고, 의존성 주입을 처리하며, 싱글톤으로 관리되는 객체들

### 📄 **일반 객체들** 
Spring이 관리하지 않고, 프록시 클래스 내부에서 `new` 연산자로 생성되어 사용되는 객체들

---

## 📦 Spring 컨테이너에 등록된 빈들

### 1. **ProxyApplication** 🚀
```java
@SpringBootApplication
public class ProxyApplication {
    // Spring Boot가 자동으로 빈 등록
}
```
- **빈 이름**: `proxyApplication`
- **등록 방식**: `@SpringBootApplication`
- **역할**: 애플리케이션 시작점, 컴포넌트 스캔 기준점

### 2. **ThreadLocalLogTrace** 📊
```java
@SpringBootApplication
public class ProxyApplication {
    @Bean
    public LogTrace logTrace() {
        return new ThreadLocalLogTrace(); // ← 이 객체가 빈으로 등록됨
    }
}
```
- **빈 이름**: `logTrace`
- **등록 방식**: `@Bean` 메서드
- **역할**: 로그 추적기, 모든 프록시가 공유

### 3. **OrderControllerV1ApiAdapter** 🌐
```java
@RestController
public class OrderControllerV1ApiAdapter {
    // Spring이 자동으로 빈 등록
}
```
- **빈 이름**: `orderControllerV1ApiAdapter`
- **등록 방식**: `@RestController` (컴포넌트 스캔)
- **역할**: HTTP 요청 처리

### 4. **InterfaceProxyConfig** ⚙️
```java
@Configuration
public class InterfaceProxyConfig {
    // Spring이 자동으로 빈 등록
}
```
- **빈 이름**: `interfaceProxyConfig`
- **등록 방식**: `@Configuration` (컴포넌트 스캔)
- **역할**: 프록시 빈들 생성 설정

### 5. **OrderControllerInterfaceProxy** 🎭
```java
@Configuration
public class InterfaceProxyConfig {
    @Bean
    public OrderControllerV1 orderController(LogTrace logTrace) {
        OrderControllerV1 impl = new OrderControllerV1Impl(...); // 일반 객체
        return new OrderControllerInterfaceProxy(impl, logTrace); // ← 이 프록시가 빈 등록
    }
}
```
- **빈 이름**: `orderController`
- **등록 방식**: `@Bean` 메서드
- **타입**: `OrderControllerV1` (인터페이스)
- **실제 클래스**: `OrderControllerInterfaceProxy`

### 6. **OrderServiceInterfaceProxy** 🎭
- **빈 이름**: `orderService`
- **등록 방식**: `@Bean` 메서드
- **타입**: `OrderServiceV1` (인터페이스)

### 7. **OrderRepositoryInterfaceProxy** 🎭
- **빈 이름**: `orderRepository`
- **등록 방식**: `@Bean` 메서드
- **타입**: `OrderRepositoryV1` (인터페이스)

---

## 📄 일반 객체들 (Spring이 관리하지 않음)

### 1. **OrderControllerV1Impl** 🏗️
```java
@Bean
public OrderControllerV1 orderController(LogTrace logTrace) {
    // 이 객체는 빈으로 등록되지 않음!
    OrderControllerV1 impl = new OrderControllerV1Impl(orderService(logTrace));
    return new OrderControllerInterfaceProxy(impl, logTrace); // impl은 프록시 내부에만 존재
}
```
- **생성 방식**: `new` 연산자
- **관리 주체**: `OrderControllerInterfaceProxy` 내부
- **접근성**: 외부에서 직접 접근 불가

### 2. **OrderServiceV1Impl** 🏗️
- **생성 방식**: `new` 연산자
- **관리 주체**: `OrderServiceInterfaceProxy` 내부

### 3. **OrderRepositoryV1Impl** 🏗️
- **생성 방식**: `new` 연산자
- **관리 주체**: `OrderRepositoryInterfaceProxy` 내부

---

## 🔗 객체 관계 분석

### Spring 빈 간의 의존성 주입
```java
// Spring이 자동으로 처리하는 의존성 주입
OrderControllerV1ApiAdapter {
    OrderControllerV1 orderController; // ← Spring이 프록시 빈 주입
}

OrderControllerInterfaceProxy {
    LogTrace logTrace; // ← Spring이 LogTrace 빈 주입
}
```

### 프록시와 구현체 관계
```java
// 프록시 클래스 내부 구조
public class OrderControllerInterfaceProxy implements OrderControllerV1 {
    private final OrderControllerV1 target; // ← 여기에 OrderControllerV1Impl 객체
    private final LogTrace logTrace;         // ← 여기에 Spring 빈 주입
    
    public String request(String itemId) {
        TraceStatus status = logTrace.begin("OrderController.request()");
        try {
            return target.request(itemId); // 실제 구현체에 위임
        } finally {
            logTrace.end(status);
        }
    }
}
```

### 일반 객체 간의 의존성
```java
// OrderControllerV1Impl 생성 시
new OrderControllerV1Impl(orderService(logTrace))
//                     ↑ 이것은 OrderServiceInterfaceProxy (Spring 빈)

// OrderServiceV1Impl 생성 시  
new OrderServiceV1Impl(orderRepository(logTrace))
//                     ↑ 이것은 OrderRepositoryInterfaceProxy (Spring 빈)
```

---

## 🎭 프록시 패턴의 특징

### 1. **투명성**
```java
// API 어댑터 관점에서는 프록시인지 구현체인지 알 수 없음
OrderControllerV1 controller; // 인터페이스 타입으로만 접근
controller.request("item"); // 실제로는 프록시 메서드 호출
```

### 2. **캡슐화**
```java
// OrderControllerV1Impl은 프록시 내부에 완전히 숨겨짐
// 외부에서는 OrderControllerInterfaceProxy를 통해서만 접근 가능
```

### 3. **단일 책임**
- **프록시**: 로그 추적 + 위임
- **구현체**: 순수한 비즈니스 로직

---

## 🔍 Spring 컨테이너 관점

### 등록된 빈 목록 (`ApplicationContext.getBeanDefinitionNames()`)
```
1. proxyApplication
2. interfaceProxyConfig  
3. orderControllerV1ApiAdapter
4. logTrace
5. orderController (실제로는 OrderControllerInterfaceProxy)
6. orderService (실제로는 OrderServiceInterfaceProxy)
7. orderRepository (실제로는 OrderRepositoryInterfaceProxy)
```

### 등록되지 않은 객체들
- `OrderControllerV1Impl` - 프록시 내부 객체
- `OrderServiceV1Impl` - 프록시 내부 객체
- `OrderRepositoryV1Impl` - 프록시 내부 객체

---

## 💡 핵심 이해사항

1. **Spring 빈**: Spring이 생명주기를 관리하는 싱글톤 객체들
2. **일반 객체**: 프록시 생성 시점에 `new`로 생성되어 프록시 내부에서만 사용
3. **프록시 패턴**: 실제 구현체를 숨기고 부가 기능(로그 추적)을 투명하게 제공
4. **의존성 주입**: Spring 빈들 간에만 이루어지며, 일반 객체는 직접 생성자로 주입
5. **접근 제어**: 외부에서는 프록시를 통해서만 비즈니스 로직에 접근 가능

이런 구조를 통해 기존 비즈니스 로직의 변경 없이 로그 추적 기능을 투명하게 적용할 수 있습니다! 🎯