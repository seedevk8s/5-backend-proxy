# Backend Proxy v1 인터페이스 추출 리팩토링

## 개요

이 커밋은 `5-backend-proxy` 프로젝트의 v1 패키지에서 기존의 구체 클래스들을 인터페이스와 구현체로 분리하는 리팩토링 작업을 수행했습니다. 이를 통해 의존성 역전 원칙(Dependency Inversion Principle)을 적용하여 코드의 유연성과 테스트 가능성을 향상시켰습니다.

## 변경 사항

### 1. OrderController 분리

**변경 전:**
- `OrderControllerV1` 클래스가 직접 비즈니스 로직을 포함

**변경 후:**
- `OrderControllerV1` 인터페이스 생성
- `OrderControllerV1Impl` 구현체 클래스 생성

### 2. OrderService 분리

**변경 전:**
- `OrderServiceV1` 클래스가 직접 Repository를 의존

**변경 후:**
- `OrderServiceV1` 인터페이스 생성
- `OrderServiceV1Impl` 구현체 클래스 생성

### 3. OrderRepository 분리

**변경 전:**
- `OrderRepositoryV1` 클래스가 구체적인 저장 로직을 포함

**변경 후:**
- `OrderRepositoryV1` 인터페이스 생성
- `OrderRepositoryV1Impl` 구현체 클래스 생성

## 아키텍처 구조

### 계층별 역할

**Controller Layer**
- HTTP 요청을 받아 Service Layer로 전달
- REST API 엔드포인트: `GET /v1/request`
- Spring의 `@RestController` 어노테이션 사용

**Service Layer**
- 비즈니스 로직 처리
- Controller와 Repository 사이의 중간 계층
- Spring의 `@Service` 어노테이션 사용

**Repository Layer**
- 데이터 저장 및 조회 담당
- 실제 저장 로직과 예외 처리 포함
- Spring의 `@Repository` 어노테이션 사용

### 의존성 관계

```
OrderControllerV1Impl → OrderServiceV1 (Interface)
OrderServiceV1Impl → OrderRepositoryV1 (Interface)
```

## 주요 기능

### 주문 처리 플로우

1. **HTTP 요청 수신**: `GET /v1/request?itemId={itemId}`
2. **Controller**: 요청을 받아 Service로 전달
3. **Service**: 비즈니스 로직 처리 후 Repository 호출
4. **Repository**: 실제 데이터 저장 수행

### 예외 처리

- `itemId`가 "ex"인 경우 `IllegalStateException("예외 발생!")` 발생
- 정상 처리 시 1초 지연 후 완료

### 응답

- 성공 시: `"ok"` 문자열 반환

## 리팩토링의 장점

### 1. 의존성 역전 (Dependency Inversion)
- 상위 모듈이 하위 모듈의 구체적인 구현에 의존하지 않음
- 인터페이스를 통한 느슨한 결합 달성

### 2. 테스트 용이성
- Mock 객체를 이용한 단위 테스트 작성 가능
- 각 계층별 독립적인 테스트 수행 가능

### 3. 확장성
- 새로운 구현체를 쉽게 추가할 수 있음
- 기존 코드 수정 없이 기능 확장 가능

### 4. 유지보수성
- 인터페이스를 통한 명확한 계약 정의
- 구현체 변경 시 영향 범위 최소화

## 파일 구조

```
src/main/java/com/choongang/proxy/app/v1/
├── OrderControllerV1.java (Interface)
├── OrderControllerV1Impl.java (Implementation)
├── OrderServiceV1.java (Interface)
├── OrderServiceV1Impl.java (Implementation)
├── OrderRepositoryV1.java (Interface)
└── OrderRepositoryV1Impl.java (Implementation)
```

## 사용된 어노테이션

- `@RestController`: REST API 컨트롤러 지정
- `@Service`: 서비스 계층 컴포넌트 지정
- `@Repository`: 데이터 접근 계층 컴포넌트 지정
- `@RequiredArgsConstructor`: Lombok을 통한 생성자 자동 생성
- `@GetMapping`: HTTP GET 요청 매핑
- `@Override`: 인터페이스 메서드 구현 명시

## 결론

이번 리팩토링을 통해 코드의 구조가 더욱 명확해지고, SOLID 원칙을 준수하는 설계로 개선되었습니다. 특히 DIP(Dependency Inversion Principle)를 적용하여 각 계층 간의 결합도를 낮추고 테스트 가능성을 높였습니다.