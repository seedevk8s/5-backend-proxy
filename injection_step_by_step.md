# 의존성 주입 "실제로는 프록시 객체가 주입됨" 단계별 분석

## 🎯 핵심 상황

```java
// 개발자가 작성한 코드
@RestController
public class OrderControllerV1ApiAdapter {
    
    private final OrderControllerV1 orderController; // ← 인터페이스 타입
    
    // 생성자 주입
    public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
        this.orderController = orderController; // ← 실제로는 프록시 객체가 주입됨
    }
}
```

---

## 🔍 단계별 상세 분석

### 1단계: 개발자가 코드 작성 👨‍💻
```java
// 개발자는 이렇게만 생각함
OrderControllerV1 orderController; // "인터페이스 타입의 필드가 필요해"
```

**개발자 관점:**
- "OrderControllerV1 인터페이스를 구현한 뭔가가 주입될 거야"
- 구체적으로 어떤 클래스인지는 모르고 관심도 없음

### 2단계: Spring이 빈 검색 🔍
```java
// Spring 내부 동작 (의사코드)
Class<?> requiredType = OrderControllerV1.class;
String[] beanNames = getBeanNamesForType(requiredType);
// 결과: ["orderController"]
```

**Spring의 검색 과정:**
1. `OrderControllerV1` 타입의 빈을 찾아라
2. Bean Registry에서 해당 타입을 구현한 빈 검색
3. `"orderController"` 빈을 발견

### 3단계: 빈 저장소에서 실제 객체 조회 📦
```java
// Spring Bean Registry 내부 상태
Map<String, Object> singletonObjects = {
    "orderController" -> OrderControllerInterfaceProxy@12345,
    "logTrace" -> ThreadLocalLogTrace@67890,
    // ...
}

// 실제 조회
Object bean = singletonObjects.get("orderController");
// 결과: OrderControllerInterfaceProxy@12345
```

**Bean Registry 상태:**
- 빈 이름: `"orderController"`
- 저장된 실제 객체: `OrderControllerInterfaceProxy` 인스턴스
- 등록 타입: `OrderControllerV1` (인터페이스)

### 4단계: 타입 호환성 검증 ✅
```java
// Spring의 타입 검증 (의사코드)
Object actualBean = OrderControllerInterfaceProxy@12345;
Class<?> requiredType = OrderControllerV1.class;

boolean isAssignable = requiredType.isAssignableFrom(actualBean.getClass());
// OrderControllerV1.isAssignableFrom(OrderControllerInterfaceProxy.class)
// 결과: true (프록시가 인터페이스를 구현하므로)
```

**다형성 원리:**
```java
// 이런 관계이므로 타입 호환 가능
public class OrderControllerInterfaceProxy implements OrderControllerV1 {
    // OrderControllerInterfaceProxy IS-A OrderControllerV1
}
```

### 5단계: 실제 객체 주입 💉
```java
// Spring이 실제로 하는 일
public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
    // orderController 매개변수에는 실제로 OrderControllerInterfaceProxy@12345가 전달됨
    this.orderController = orderController; 
    // 필드에도 OrderControllerInterfaceProxy@12345가 저장됨
}
```

**메모리 상태:**
```
OrderControllerV1ApiAdapter@99999 {
    orderController: OrderControllerInterfaceProxy@12345 {
        target: OrderControllerV1Impl@11111,
        logTrace: ThreadLocalLogTrace@67890
    }
}
```

---

## 🎭 런타임 동작 검증

### 실제 타입 확인
```java
@RestController
public class OrderControllerV1ApiAdapter {
    
    public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
        // 실제 주입된 객체의 타입 확인
        System.out.println("주입된 객체 클래스: " + orderController.getClass());
        // 출력: class OrderControllerInterfaceProxy
        
        System.out.println("인터페이스 구현 여부: " + 
            (orderController instanceof OrderControllerV1));
        // 출력: true
        
        System.out.println("프록시 객체 여부: " + 
            (orderController instanceof OrderControllerInterfaceProxy));
        // 출력: true
        
        this.orderController = orderController;
    }
}
```

### 메서드 호출 시 동작
```java
public String request(@RequestParam("itemId") String itemId) {
    // 개발자는 이렇게 호출
    return orderController.request(itemId);
    
    // 실제로는 이런 일이 벌어짐:
    // 1. OrderControllerInterfaceProxy.request(itemId) 호출
    // 2. 프록시가 로그 추적 시작
    // 3. target.request(itemId) → OrderControllerV1Impl.request(itemId) 호출
    // 4. 프록시가 로그 추적 종료
    // 5. 결과 반환
}
```

---

## 🤔 왜 이런 방식을 사용할까?

### 1. **투명성 (Transparency)**
```java
// 개발자는 프록시 존재를 모르고도 사용 가능
OrderControllerV1 controller; // 인터페이스만 알면 됨
controller.request("item");    // 프록시든 실제 구현체든 상관없이 호출
```

### 2. **교체 가능성 (Substitutability)**
```java
// 설정만 바꾸면 다른 구현체로 교체 가능
@Bean
public OrderControllerV1 orderController() {
    // return new OrderControllerInterfaceProxy(...);     // 프록시 버전
    // return new OrderControllerV1Impl(...);             // 직접 구현체
    // return new CachedOrderController(...);             // 캐시 버전
    // return new SecurityOrderController(...);           // 보안 버전
}
```

### 3. **관심사 분리 (Separation of Concerns)**
```java
// API 어댑터는 HTTP 처리에만 집중
// 로그 추적은 프록시가 담당
// 비즈니스 로직은 구현체가 담당
```

---

## 💡 핵심 포인트

1. **개발자 관점**: `OrderControllerV1` 인터페이스 타입으로만 인식
2. **Spring 관점**: `OrderControllerInterfaceProxy` 객체를 주입
3. **런타임 관점**: 프록시 객체가 로그 추적 + 위임 처리
4. **다형성 활용**: 인터페이스 타입으로 선언, 구현체로 동작
5. **투명한 프록시**: 클라이언트 코드 변경 없이 부가 기능 제공

## 🎯 결론

> **"실제로는 프록시 객체가 주입됨"**의 의미:
> 
> 개발자가 `OrderControllerV1` 인터페이스 타입으로 선언했지만,  
> Spring이 실제로 주입하는 것은 `OrderControllerInterfaceProxy` 인스턴스이며,  
> 이 프록시가 로그 추적 기능을 투명하게 제공한다.

이런 방식으로 **기존 코드 변경 없이** 로그 추적 기능을 적용할 수 있습니다! 🚀✨