# Spring 애플리케이션의 HTTP 요청 처리 과정

## 개요

이 문서는 `http://localhost:8080/v2/request?itemId=choongang` 요청이 SpringBoot 애플리케이션에서 처리되는 전체 과정을 설명합니다. 코드는 V2 버전의 컨트롤러, 서비스, 리포지토리 구조를 기반으로 합니다.

## 1. 애플리케이션 시작 과정

1. **ProxyApplication 클래스 실행**
   - `SpringApplication.run(ProxyApplication.class, args)` 호출
   - Spring 컨테이너(ApplicationContext) 초기화

2. **빈 등록**
   - `@Import(AppV2Config.class)`를 통해 AppV2Config에 정의된 빈들이 등록됨
   - AppV2Config에서 다음 빈들을 등록:
     - `orderControllerV2`
     - `orderServiceV2`
     - `orderRepositoryV2`
   - 빈 간의 의존성 주입이 이루어짐

3. **CommandLineRunner 실행**
   - 애플리케이션 시작 후 등록된 빈 목록 중 'order', 'v2', 'controller'를 포함하는 빈들을 출력

## 2. HTTP 요청 처리 과정

브라우저에서 `http://localhost:8080/v2/request?itemId=choongang`를 요청했을 때의 처리 과정:

1. **요청 수신**
   - Spring의 DispatcherServlet이 HTTP 요청을 수신
   - URL 경로 `/v2/request`와 파라미터 `itemId=choongang` 파싱

2. **핸들러 매핑**
   - `@RequestMapping` 및 `@GetMapping("/v2/request")` 애노테이션을 기반으로 OrderControllerV2의 request 메서드가 호출 대상으로 결정됨

3. **컨트롤러 실행**
   - `OrderControllerV2.request("choongang")` 메서드 호출
   - 컨트롤러는 주입받은 OrderServiceV2 인스턴스의 메서드 호출

4. **서비스 계층 처리**
   - `OrderServiceV2.orderItem("choongang")` 메서드 실행
   - 서비스는 주입받은 OrderRepositoryV2 인스턴스의 메서드 호출

5. **리포지토리 계층 처리**
   - `OrderRepositoryV2.save("choongang")` 메서드 실행
   - "ex"가 아니므로 예외 발생하지 않음
   - `Thread.sleep(1000)` 호출로 1초간 대기 (의도적인 지연)

6. **응답 반환**
   - 리포지토리 → 서비스 → 컨트롤러로 처리 완료 전파
   - 컨트롤러가 문자열 "ok" 반환
   - Spring이 HTTP 200 OK 응답과 함께 "ok" 본문을 클라이언트에 반환

## 3. 주요 코드 설명

### V2 구조의 특징
- 순수 자바 클래스로 구현 (스프링 어노테이션 최소화)
- 컨트롤러만 Spring Web 관련 어노테이션 사용
- 설정 클래스(AppV2Config)를 통한 수동 빈 등록 방식 사용

### ProxyApplication 클래스
- `@SpringBootApplication(scanBasePackages = {"com.choongang.proxy.app"})`: component scan 범위 지정
- `@Import(AppV2Config.class)`: 수동 빈 설정 클래스 가져오기
- CommandLineRunner를 통해 애플리케이션 시작 시 등록된 빈 목록 확인 가능

### 주요 클래스 간 호출 관계
OrderControllerV2 → OrderServiceV2 → OrderRepositoryV2

## 4. 예상 실행 결과

1. 웹 브라우저에서 `http://localhost:8080/v2/request?itemId=choongang` 요청
2. 약 1초의 지연 후(리포지토리의 sleep 메서드)
3. 화면에 "ok" 문자열이 표시됨

## 5. 예외 처리 시나리오

만약 `http://localhost:8080/v2/request?itemId=ex`로 요청할 경우:
- OrderRepositoryV2의 save 메서드에서 `throw new IllegalStateException("예외 발생!");` 실행됨
- 예외가 컨트롤러까지 전파되어 클라이언트에 500 에러 응답 반환

## 6. 프록시 관련 특이사항

- 프로젝트명이 "ProxyApplication"인 것으로 보아 프록시 패턴을 학습하기 위한 예제로 추정됨
- 현재 코드에는 프록시 패턴이 적용되지 않은 상태
- 추후 AOP나 프록시 패턴을 적용하면 OrderServiceV2나 OrderRepositoryV2의 기능을 가로채거나 확장 가능
