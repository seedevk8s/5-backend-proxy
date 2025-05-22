# Spring MVC 요청 처리 과정 분석

## 📋 요청 개요
- **URL**: `http://localhost:8080/v2/request?itemId=choongang`
- **Method**: GET
- **현재 상태**: 404 NOT_FOUND 에러 발생

## 🔄 현재 실행 과정 (문제 상황)

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant DS as DispatcherServlet
    participant HM as HandlerMapping
    participant RH as ResourceHttpRequestHandler
    participant EC as ErrorController

    Client->>DS: GET /v2/request?itemId=choongang
    Note over DS: 요청 접수 및 로깅<br/>TRACE: GET "/v2/request?itemId=choongang"

    DS->>HM: 핸들러 검색
    Note over HM: @Controller/@RestController가 있는<br/>핸들러를 찾음

    HM-->>DS: 핸들러 없음!
    Note over HM: /v2/request 매핑이<br/>등록되지 않음

    DS->>HM: 기본 핸들러 검색
    HM->>DS: ResourceHttpRequestHandler 반환
    Note over DS,RH: 정적 리소스 핸들러로<br/>매핑됨 (잘못된 라우팅)

    DS->>RH: 정적 파일 검색
    Note over RH: classpath에서<br/>v2/request 파일 검색

    RH-->>DS: Resource not found
    Note over RH: DEBUG: Resource not found

    DS->>EC: 404 에러 처리
    Note over EC: /error로 리다이렉트

    EC->>Client: 404 NOT_FOUND 응답
    Note over Client: 에러 페이지 반환
```

## ❌ 문제 원인 분석

### 1. Bean 등록 상태
```java
// AppV2Config.java - ✅ Bean으로 등록됨
@Configuration
public class AppV2Config {
    @Bean
    public OrderControllerV2 orderControllerV2() {
        return new OrderControllerV2(orderServiceV2());
    }
}
```

### 2. 컨트롤러 어노테이션 누락
```java
// OrderControllerV2.java - ❌ 컨트롤러 어노테이션 없음
@Slf4j
@RequestMapping    // 이것만으론 부족!
@ResponseBody      // 이것도 부족!
public class OrderControllerV2 {
    @GetMapping("/v2/request")  // 매핑이 등록되지 않음
    public String request(String itemId) {
        // ...
    }
}
```

### 3. Spring MVC 매핑 테이블 상태
```
현재 매핑 테이블:
┌─────────────────┬──────────────────┐
│ URL Pattern     │ Handler          │
├─────────────────┼──────────────────┤
│ /v2/request     │ (없음!)          │
│ /error          │ ErrorController  │
│ /**             │ ResourceHandler  │
└─────────────────┴──────────────────┘
```

## ✅ 해결 후 예상 실행 과정

```mermaid
sequenceDiagram
    participant Client as 클라이언트
    participant DS as DispatcherServlet
    participant HM as HandlerMapping
    participant Controller as OrderControllerV2
    participant Service as OrderServiceV2
    participant Repo as OrderRepositoryV2

    Client->>DS: GET /v2/request?itemId=choongang
    Note over DS: 요청 접수

    DS->>HM: 핸들러 검색
    Note over HM: @RestController가 있는<br/>핸들러를 찾음

    HM->>DS: OrderControllerV2.request() 반환
    Note over DS,Controller: 올바른 핸들러 매핑!

    DS->>Controller: request("choongang") 호출
    Note over Controller: @GetMapping("/v2/request")<br/>매핑 처리

    Controller->>Service: orderItem("choongang")
    Service->>Repo: save("choongang")
    
    Note over Repo: 1초 sleep() 실행<br/>"choongang"은 정상 처리

    Repo-->>Service: 저장 완료
    Service-->>Controller: 처리 완료
    Controller-->>DS: "ok" 반환

    DS->>Client: 200 OK<br/>Response Body: "ok"
```

## 🛠️ 해결 방법

### 1. 컨트롤러 어노테이션 추가
```java
@Slf4j
@RestController  // 👈 이 어노테이션 추가!
@RequestMapping
public class OrderControllerV2 {
    // 기존 코드 그대로...
}
```

### 2. 해결 후 매핑 테이블
```
해결 후 매핑 테이블:
┌─────────────────┬──────────────────────────┐
│ URL Pattern     │ Handler                  │
├─────────────────┼──────────────────────────┤
│ /v2/request     │ OrderControllerV2.request│ ✅
│ /error          │ ErrorController          │
│ /**             │ ResourceHandler          │
└─────────────────┴──────────────────────────┘
```

### 3. 애플리케이션 시작 시 확인할 로그
```
INFO : Mapped "{[/v2/request],methods=[GET]}" onto 
       public java.lang.String OrderControllerV2.request(String)
```

## 🔍 핵심 개념

### Bean 등록 vs 컨트롤러 등록
```
Bean 등록 (Spring Container)
    ↓
    객체 생성 및 의존성 주입
    ↓
    @Autowired 등으로 사용 가능

컨트롤러 등록 (Spring MVC)
    ↓
    @Controller/@RestController 스캔
    ↓
    @RequestMapping 등 분석
    ↓
    URL 매핑 테이블 생성
    ↓
    HTTP 요청 처리 가능
```

### 어노테이션의 역할
- **`@Bean`**: Spring 컨테이너에 객체 등록
- **`@Controller`**: Spring MVC에 컨트롤러로 등록
- **`@RestController`**: `@Controller` + `@ResponseBody`
- **`@RequestMapping`**: URL 매핑 정보 제공

## 📝 결론

현재 문제는 **Bean으로는 등록되어 있지만, Spring MVC 컨트롤러로는 등록되지 않은 상태**입니다. `@RestController` 어노테이션을 추가하면 Spring MVC가 해당 클래스를 컨트롤러로 인식하여 URL 매핑을 등록하게 됩니다.