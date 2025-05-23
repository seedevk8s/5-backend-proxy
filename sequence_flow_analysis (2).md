# 프록시 로그 추적 시스템 시퀀스 실행 흐름 분석

## 개요
`http://localhost:8080/v1/request?itemId=choongang` 요청이 처리되는 과정을 시퀀스 다이어그램 순서대로 상세히 분석합니다.

## 전체 실행 흐름 요약

### 참여 객체
- **Client**: HTTP 요청을 보내는 클라이언트
- **OrderControllerV1ApiAdapter**: HTTP 요청을 받는 실제 컨트롤러 (`@RestController`)
- **OrderControllerInterfaceProxy**: 컨트롤러 프록시 (로그 추적 기능)
- **ThreadLocalLogTrace**: 로그 추적기 (ProxyApplication에서 @Bean으로 등록)
- **OrderControllerV1Impl**: 실제 컨트롤러 구현체
- **OrderServiceInterfaceProxy**: 서비스 프록시 (로그 추적 기능)
- **OrderServiceV1Impl**: 실제 서비스 구현체
- **OrderRepositoryInterfaceProxy**: 리포지토리 프록시 (로그 추적 기능)
- **OrderRepositoryV1Impl**: 실제 리포지토리 구현체

---

## 단계별 실행 흐름

### 1단계: HTTP 요청 수신
```
Client → OrderControllerV1ApiAdapter: GET /v1/request?itemId=choongang
```
- **시점**: 2025-05-23T17:23:06.555+09:00
- **동작**: 클라이언트가 HTTP GET 요청을 전송
- **처리**: `OrderControllerV1ApiAdapter`가 Spring MVC를 통해 요청을 수신

### 2단계: API 어댑터가 프록시에 위임 (상세 분석)
```
OrderControllerV1ApiAdapter → OrderControllerInterfaceProxy: request("choongang")
```

#### 🔍 **코드 레벨 상세 분석**

**OrderControllerV1ApiAdapter 내부 동작:**
```java
@RestController
public class OrderControllerV1ApiAdapter {
    
    // Spring이 OrderControllerV1 타입의 빈을 찾아서 주입
    // InterfaceProxyConfig에서 생성한 OrderControllerInterfaceProxy 인스턴스가 주입됨
    private final OrderControllerV1 orderController;
    
    // 생성자 주입
    public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
        this.orderController = orderController; // 실제로는 프록시 객체가 주입됨
    }
    
    @GetMapping("/v1/request")
    public String request(@RequestParam("itemId") String itemId) {
        // 여기서 실제 메서드 호출이 발생
        return orderController.request(itemId); // 프록시의 request() 메서드 호출
        //     ↑ 이 부분이 시퀀스 다이어그램의 화살표에 해당
    }
}
```

#### 🔄 **Spring 의존성 주입 과정**

1. **빈 등록 시점 (애플리케이션 시작)**:
```java
@Configuration
public class InterfaceProxyConfig {
    @Bean
    public OrderControllerV1 orderController(LogTrace logTrace) {
        OrderControllerV1 target = new OrderControllerV1Impl(orderService(logTrace));
        return new OrderControllerInterfaceProxy(target, logTrace); // 프록시 객체 생성 후 빈으로 등록
    }
}
```

2. **의존성 주입 시점**:
```java
// Spring이 OrderControllerV1 타입의 빈을 찾음
// InterfaceProxyConfig에서 등록한 OrderControllerInterfaceProxy 인스턴스를 발견
// 해당 인스턴스를 OrderControllerV1ApiAdapter에 주입
```

#### 📞 **실제 메서드 호출 과정**

**호출 이전 상태:**
```java
// orderController 필드에는 실제로 이런 객체가 들어있음
OrderControllerInterfaceProxy proxyInstance = new OrderControllerInterfaceProxy(
    new OrderControllerV1Impl(...), // 실제 구현체
    threadLocalLogTrace              // 로그 추적기
);
```

**호출 순간:**
```java
// API 어댑터에서 이 코드가 실행될 때:
return orderController.request("choongang");

// 실제로는 다음과 같이 변환됨:
return proxyInstance.request("choongang");
```

#### 🎭 **프록시 패턴의 투명성**

**API 어댑터 관점:**
- API 어댑터는 자신이 프록시 객체를 호출하는지 모름
- `OrderControllerV1` 인터페이스로만 바라봄
- 단순히 `orderController.request("choongang")` 호출

**실제 실행 객체:**
- `OrderControllerInterfaceProxy` 인스턴스의 `request()` 메서드가 실행됨
- 프록시가 로그 추적 로직을 먼저 실행
- 그 다음 실제 구현체에 위임

#### 🔗 **인터페이스 기반 의존성 주입**

```java
// 인터페이스 정의
public interface OrderControllerV1 {
    String request(String itemId);
}

// 실제 구현체
public class OrderControllerV1Impl implements OrderControllerV1 {
    public String request(String itemId) { /* 비즈니스 로직 */ }
}

// 프록시 클래스
public class OrderControllerInterfaceProxy implements OrderControllerV1 {
    private final OrderControllerV1 target; // 실제 구현체 참조
    
    public String request(String itemId) {
        // 로그 추적 로직
        // target.request(itemId) 호출 (실제 구현체에 위임)
    }
}

// API 어댑터는 인터페이스 타입으로만 의존
public class OrderControllerV1ApiAdapter {
    private final OrderControllerV1 orderController; // 인터페이스 타입
    // 실제 주입되는 것은 OrderControllerInterfaceProxy 인스턴스
}
```

#### ⚡ **호출 시점의 실제 동작**

1. **HTTP 요청 수신**: `GET /v1/request?itemId=choongang`
2. **Spring MVC 라우팅**: `OrderControllerV1ApiAdapter.request()` 메서드 매핑
3. **매개변수 바인딩**: `@RequestParam`으로 `"choongang"` 추출
4. **프록시 메서드 호출**: `orderController.request("choongang")` 실행
5. **실제 타겟**: `OrderControllerInterfaceProxy.request("choongang")` 실행됨

#### 🧩 **어댑터 패턴의 역할**

**HTTP 인터페이스 → 비즈니스 인터페이스 변환:**
```java
// HTTP 인터페이스 (Spring MVC)
@GetMapping("/v1/request")
public String request(@RequestParam("itemId") String itemId)

// 비즈니스 인터페이스 (도메인 로직)
public String request(String itemId)
```

**API 어댑터가 하는 일:**
1. HTTP 요청 파라미터 추출
2. 비즈니스 메서드 매개변수로 변환
3. 비즈니스 로직 호출 (프록시를 통해)
4. 비즈니스 결과를 HTTP 응답으로 변환

#### 🎯 **핵심 포인트**

- **투명한 프록시**: API 어댑터는 프록시 존재를 모름
- **인터페이스 기반**: 강한 결합 없이 교체 가능한 구조
- **Spring DI**: 설정에 따라 실제 구현체 또는 프록시 주입 가능
- **어댑터 역할**: HTTP 계층과 비즈니스 계층 간의 변환기 역할

### 3단계: 컨트롤러 프록시 로그 추적 시작
```
OrderControllerInterfaceProxy → ThreadLocalLogTrace: begin("OrderController.request()")
ThreadLocalLogTrace → OrderControllerInterfaceProxy: TraceStatus [12564b6b]
```
- **시점**: 2025-05-23T17:23:06.555+09:00
- **동작**: 컨트롤러 프록시가 로그 추적을 시작
- **로그 추적기**: ProxyApplication에서 @Bean으로 등록한 ThreadLocalLogTrace 인스턴스 사용
- **생성**: 트레이스 ID `[12564b6b]` 생성
- **로그 출력**: `[12564b6b] OrderController.request()`

### 4단계: 컨트롤러 프록시가 실제 구현체 호출
```
OrderControllerInterfaceProxy → OrderControllerV1Impl: request("choongang")
```
- **동작**: 프록시가 실제 컨트롤러 구현체에 메서드 호출 위임
- **패턴**: 프록시 패턴의 위임(Delegation) 적용

### 5단계: 컨트롤러가 서비스 프록시 호출
```
OrderControllerV1Impl → OrderServiceInterfaceProxy: orderItem("choongang")
```
- **동작**: 컨트롤러가 서비스 계층 호출
- **특징**: 실제 구현체도 프록시 빈을 주입받아 사용

### 6단계: 서비스 프록시 로그 추적 시작
```
OrderServiceInterfaceProxy → ThreadLocalLogTrace: begin("OrderService.orderItem()")
ThreadLocalLogTrace → OrderServiceInterfaceProxy: TraceStatus [12564b6b]
```
- **시점**: 2025-05-23T17:23:06.555+09:00
- **동작**: 서비스 프록시가 로그 추적을 시작
- **트레이스 ID**: 동일한 `[12564b6b]` 사용 (같은 요청 스레드)
- **로그 출력**: `[12564b6b] |-->OrderService.orderItem()`
- **계층 표시**: `|-->` 로 한 단계 깊어진 호출 표현

### 7단계: 서비스 프록시가 실제 구현체 호출
```
OrderServiceInterfaceProxy → OrderServiceV1Impl: orderItem("choongang")
```
- **동작**: 서비스 프록시가 실제 서비스 구현체에 위임

### 8단계: 서비스가 리포지토리 프록시 호출
```
OrderServiceV1Impl → OrderRepositoryInterfaceProxy: save("choongang")
```
- **동작**: 서비스가 데이터 접근을 위해 리포지토리 계층 호출

### 9단계: 리포지토리 프록시 로그 추적 시작
```
OrderRepositoryInterfaceProxy → ThreadLocalLogTrace: begin("OrderRepository.save()")
ThreadLocalLogTrace → OrderRepositoryInterfaceProxy: TraceStatus [12564b6b]
```
- **시점**: 2025-05-23T17:23:06.555+09:00
- **동작**: 리포지토리 프록시가 로그 추적을 시작
- **로그 출력**: `[12564b6b] |   |-->OrderRepository.save()`
- **계층 표시**: `|   |-->` 로 두 단계 깊어진 호출 표현

### 10단계: 리포지토리 프록시가 실제 구현체 호출
```
OrderRepositoryInterfaceProxy → OrderRepositoryV1Impl: save("choongang")
```
- **동작**: 리포지토리 프록시가 실제 구현체에 위임
- **비즈니스 로직**: 1초 대기 로직 실행 (시뮬레이션)

### 11단계: 리포지토리 처리 완료 및 반환
```
OrderRepositoryV1Impl → OrderRepositoryInterfaceProxy: 처리 완료
```
- **동작**: 실제 리포지토리 구현체가 작업 완료 후 결과 반환
- **소요 시간**: 약 1015ms (1초 + 처리 시간)

### 12단계: 리포지토리 프록시 로그 추적 종료
```
OrderRepositoryInterfaceProxy → ThreadLocalLogTrace: end(TraceStatus, null)
ThreadLocalLogTrace → OrderRepositoryInterfaceProxy: 로그 출력
```
- **시점**: 2025-05-23T17:23:07.570+09:00
- **동작**: 리포지토리 프록시가 로그 추적을 종료
- **로그 출력**: `[12564b6b] |   |<--OrderRepository.save() time=1015ms`
- **시간 계산**: 시작 시점부터 종료 시점까지의 실행 시간 측정

### 13단계: 리포지토리 프록시가 서비스로 결과 반환
```
OrderRepositoryInterfaceProxy → OrderServiceV1Impl: 결과 반환
```
- **동작**: 프록시가 서비스 계층으로 처리 결과 전달

### 14단계: 서비스가 서비스 프록시로 결과 반환
```
OrderServiceV1Impl → OrderServiceInterfaceProxy: 결과 반환
```
- **동작**: 실제 서비스 구현체가 프록시로 결과 전달

### 15단계: 서비스 프록시 로그 추적 종료
```
OrderServiceInterfaceProxy → ThreadLocalLogTrace: end(TraceStatus, null)
ThreadLocalLogTrace → OrderServiceInterfaceProxy: 로그 출력
```
- **시점**: 2025-05-23T17:23:07.570+09:00
- **동작**: 서비스 프록시가 로그 추적을 종료
- **로그 출력**: `[12564b6b] |<--OrderService.orderItem() time=1015ms`
- **시간 일치**: 하위 계층과 동일한 실행 시간 (연속적 실행)

### 16단계: 서비스 프록시가 컨트롤러로 결과 반환
```
OrderServiceInterfaceProxy → OrderControllerV1Impl: 결과 반환
```
- **동작**: 서비스 프록시가 컨트롤러 계층으로 결과 전달

### 17단계: 컨트롤러가 컨트롤러 프록시로 결과 반환
```
OrderControllerV1Impl → OrderControllerInterfaceProxy: 결과 반환
```
- **동작**: 실제 컨트롤러 구현체가 프록시로 결과 전달

### 18단계: 컨트롤러 프록시 로그 추적 종료
```
OrderControllerInterfaceProxy → ThreadLocalLogTrace: end(TraceStatus, null)
ThreadLocalLogTrace → OrderControllerInterfaceProxy: 로그 출력
```
- **시점**: 2025-05-23T17:23:07.570+09:00
- **동작**: 컨트롤러 프록시가 로그 추적을 종료
- **로그 출력**: `[12564b6b] OrderController.request() time=1015ms`
- **최상위 계층**: 들여쓰기 없이 최상위 계층임을 표시

### 19단계: 컨트롤러 프록시가 API 어댑터로 결과 반환
```
OrderControllerInterfaceProxy → OrderControllerV1ApiAdapter: 결과 반환
```
- **동작**: 프록시가 API 어댑터로 비즈니스 로직 처리 결과 전달

### 20단계: API 어댑터가 클라이언트로 HTTP 응답
```
OrderControllerV1ApiAdapter → Client: HTTP 응답
```
- **동작**: API 어댑터가 HTTP 응답을 클라이언트에 전송
- **완료**: 전체 요청-응답 사이클 완료

---

## 로그 추적 패턴 분석

### 로그 계층 구조
```
[12564b6b] OrderController.request()                    (최상위 - 들여쓰기 없음)
[12564b6b] |-->OrderService.orderItem()                 (1단계 깊이)
[12564b6b] |   |-->OrderRepository.save()              (2단계 깊이)
[12564b6b] |   |<--OrderRepository.save() time=1015ms  (2단계 복귀)
[12564b6b] |<--OrderService.orderItem() time=1015ms    (1단계 복귀)
[12564b6b] OrderController.request() time=1015ms       (최상위 복귀)
```

### ThreadLocal 활용
- **트레이스 ID 공유**: 동일한 요청 스레드 내에서 `[12564b6b]` 공유
- **계층 관리**: ProxyApplication에서 등록한 ThreadLocalLogTrace를 통해 호출 깊이(`level`) 관리
- **시간 측정**: 각 계층별 독립적인 시작/종료 시간 추적
- **빈 공유**: 모든 프록시 클래스가 동일한 ThreadLocalLogTrace 인스턴스 사용

### 프록시 체인 패턴
1. **API 어댑터**: HTTP 요청 수신
2. **프록시 계층**: 로그 추적 + 위임
3. **구현 계층**: 실제 비즈니스 로직
4. **역순 반환**: 구현 → 프록시 → API 어댑터 → 클라이언트

---

## 성능 분석

### 실행 시간
- **전체 처리 시간**: 1015ms
- **각 계층 시간**: 모든 계층에서 동일한 1015ms 기록
- **병목 지점**: `OrderRepositoryV1Impl.save()` 메서드의 1초 대기

### 로그 추적 오버헤드
- **프록시 생성**: 컴파일 타임에 완료 (런타임 오버헤드 없음)
- **로그 기록**: ThreadLocal 접근 및 문자열 생성 비용
- **메서드 위임**: 직접 메서드 호출로 리플렉션 오버헤드 없음

## 결론

이 시퀀스는 인터페이스 기반 프록시 패턴을 통해 완벽한 로그 추적 시스템을 구현한 사례입니다. ProxyApplication에서 ThreadLocalLogTrace를 @Bean으로 등록하여 모든 프록시 클래스가 동일한 로그 추적기 인스턴스를 공유하며, 각 계층별로 명확한 책임 분리와 함께 횡단 관심사(로그 추적)를 효과적으로 적용했습니다. ThreadLocal을 활용해 멀티스레드 환경에서 안전한 추적 기능을 제공합니다.