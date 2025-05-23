# TimeDecorator 추가 - 데코레이터 패턴 구현

## 개요
이 커밋에서는 기존 데코레이터 패턴 구현에 **실행 시간 측정 기능**을 제공하는 `TimeDecorator` 클래스를 추가했습니다. 데코레이터 패턴을 통해 핵심 비즈니스 로직에 영향을 주지 않고 부가 기능을 동적으로 추가할 수 있습니다.

## 변경 사항

### 1. TimeDecorator 클래스 추가
**파일 위치**: `src/main/java/com/choongang/proxy/pureproxy/decorator/code/TimeDecorator.java`

```java
@Slf4j
public class TimeDecorator implements Component {
    private Component component; // 실제 객체를 참조하는 필드
    
    public TimeDecorator(Component component) { // 생성자에서 실제 객체를 주입받음
        this.component = component;
    }
    
    @Override
    public String operation() {
        log.info("TimeDecorator 실행");
        long startTime = System.currentTimeMillis(); // 시작 시간 기록
        
        String result = component.operation(); // 실제 객체의 메서드 호출
        
        long endTime = System.currentTimeMillis(); // 종료 시간 기록
        long duration = endTime - startTime; // 경과 시간 계산
        
        log.info("TimeDecorator 경과 시간: {}ms", duration); // 경과 시간 출력
        return result;
    }
}
```

### 2. 테스트 케이스 추가
**파일 위치**: `src/test/java/com/choongang/proxy/pureproxy/decorator/DecoratorPatternTest.java`

새로운 테스트 메서드 `decoratorTest2()`를 추가하여 TimeDecorator의 동작을 검증합니다.

```java
@Test
void decoratorTest2() {
    // given
    Component realComponent = new RealComponent();
    Component messageDecorator = new MessageDecorator(realComponent);
    Component timeDecorator = new TimeDecorator(messageDecorator);
    DecoratorPatternClient client = new DecoratorPatternClient(timeDecorator);
    
    // when
    client.execute();
    
    // then
    // log.info("result = {}", result);
}
```

## 데코레이터 패턴의 특징

### 핵심 구성 요소
1. **Component (인터페이스)**: 모든 컴포넌트가 구현해야 하는 공통 인터페이스
2. **RealComponent**: 실제 비즈니스 로직을 수행하는 핵심 컴포넌트
3. **Decorator들**: 부가 기능을 제공하는 데코레이터 클래스들
   - **MessageDecorator**: 메시지를 꾸며주는 기능
   - **TimeDecorator**: 실행 시간을 측정하는 기능

### 동작 흐름
```
Client → TimeDecorator → MessageDecorator → RealComponent
```

1. 클라이언트가 `TimeDecorator`의 `operation()` 호출
2. `TimeDecorator`가 시작 시간 기록 후 `MessageDecorator`의 `operation()` 호출
3. `MessageDecorator`가 메시지 처리 후 `RealComponent`의 `operation()` 호출
4. `RealComponent`가 실제 비즈니스 로직 수행
5. 결과가 역순으로 전달되며, `TimeDecorator`가 경과 시간 계산 및 로그 출력

## 장점

### 1. 단일 책임 원칙 (SRP)
각 데코레이터는 하나의 책임만 가집니다:
- `TimeDecorator`: 실행 시간 측정
- `MessageDecorator`: 메시지 꾸미기

### 2. 개방-폐쇄 원칙 (OCP)
기존 코드를 수정하지 않고 새로운 기능을 추가할 수 있습니다.

### 3. 유연한 조합
데코레이터들을 자유롭게 조합하여 다양한 기능을 구성할 수 있습니다.

### 4. 런타임 동적 기능 추가
컴파일 타임이 아닌 런타임에 객체의 기능을 동적으로 확장할 수 있습니다.

## 사용 예시

```java
// 기본 컴포넌트만 사용
Component basic = new RealComponent();

// 메시지 데코레이터만 추가
Component withMessage = new MessageDecorator(new RealComponent());

// 시간 측정 데코레이터만 추가
Component withTime = new TimeDecorator(new RealComponent());

// 두 데코레이터 모두 추가 (메시지 + 시간 측정)
Component withBoth = new TimeDecorator(
    new MessageDecorator(new RealComponent())
);
```

## 실행 결과 예시

`decoratorTest2()` 실행 시 예상되는 로그:
```
TimeDecorator 실행
MessageDecorator 실행
RealComponent 실행
TimeDecorator 경과 시간: 5ms
```

## 결론

`TimeDecorator` 추가로 기존 시스템에 성능 모니터링 기능을 비침투적으로 추가할 수 있게 되었습니다. 이는 데코레이터 패턴의 핵심 가치인 **기능의 동적 확장**과 **기존 코드의 무수정 원칙**을 잘 보여주는 사례입니다.