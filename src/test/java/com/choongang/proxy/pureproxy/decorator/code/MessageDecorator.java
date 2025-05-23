package com.choongang.proxy.pureproxy.decorator.code;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageDecorator implements Component {

    private Component component; // 실제 객체를 참조하는 필드

    public MessageDecorator(Component component) { // 생성자에서 실제 객체를 주입받음
        this.component = component;
    }

    @Override
    public String operation() {
        log.info("MessageDecorator 실행");

        // Decorator 기능 추가
        String result = component.operation(); // 실제 객체의 메서드 호출
        String decoratedResult = "*****" + result + "*****"; // Decorator 기능 추가
        log.info("MessageDecorator 꾸미기 적용 전 = {}, 적용 후 = {}", result, decoratedResult);
        return decoratedResult;
    }
}
