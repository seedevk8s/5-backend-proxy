# 프록시 로그 추적 시스템 시퀀스 실행 흐름 분석

## 개요
`http://localhost:8080/v1/request?itemId=choongang` 요청이 처리되는 과정을 시퀀스 다이어그램 순서대로 상세히 분석합니다.

## 전체 실행 흐름 요약

### 참여 객체
- **Client**: HTTP 요청을 보내는 클라이언트
- **OrderControllerV1ApiAdapter**: HTTP 요청을 받는 실제 컨트롤러 (`@RestController`)
- **OrderControllerInterfaceProxy**: 컨트롤러 프록시 (로그 추적 기능)
- **ThreadLocalLogTrace**: 로그 추적기
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

### 2단계: API 어댑터가 프록시에 위임
```
OrderControllerV1ApiAdapter → OrderControllerInterfaceProxy: request("choongang")
```
- **동작**: API 어댑터가 주입받은 프록시 빈에 요청을 위임
- **매개변수**: `itemId = "choongang"`
- **목적**: HTTP 계층과 비즈니스 로직 계층 분리

### 3단계: 컨트롤러 프록시 로그 추적 시작
```
OrderControllerInterfaceProxy → ThreadLocalLogTrace: begin("OrderController.request()")
ThreadLocalLogTrace → OrderControllerInterfaceProxy: TraceStatus [12564b6b]
```
- **시점**: 2025-05-23T17:23:06.555+09:00
- **동작**: 컨트롤러 프록시가 로그 추적을 시작
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
- **계층 관리**: ThreadLocal을 통해 호출 깊이(`level`) 관리
- **시간 측정**: 각 계층별 독립적인 시작/종료 시간 추적

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

이 시퀀스는 인터페이스 기반 프록시 패턴을 통해 완벽한 로그 추적 시스템을 구현한 사례입니다. 각 계층별로 명확한 책임 분리와 함께 횡단 관심사(로그ᄙ적)를 효과적으로 적용했으며, ThreadLocal을 활용해 멀티스레드 환경에서 안전한 추적 기능을 제공합니다.