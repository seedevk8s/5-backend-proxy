package com.choongang.proxy.pureproxy.decorator;

import com.choongang.proxy.pureproxy.decorator.code.Component;
import com.choongang.proxy.pureproxy.decorator.code.DecoratorPatternClient;
import com.choongang.proxy.pureproxy.decorator.code.MessageDecorator;
import com.choongang.proxy.pureproxy.decorator.code.RealComponent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * Decorator 패턴 테스트
 * Decorator 패턴은 기능을 동적으로 추가할 수 있는 패턴이다.
 * Decorator는 Component 인터페이스를 구현하고, Component를 감싸는 역할을 한다.
 * Decorator는 Component의 기능을 확장할 수 있다.
 * Decorator는 Component를 상속받지 않고, Component를 포함하는 방식으로 구현한다.
 */
@Slf4j
public class DecoratorPatternTest {

    @Test
    void noDecoratorTest() {
        // given
        Component realComponent = new RealComponent();
        DecoratorPatternClient client = new DecoratorPatternClient(realComponent);

        // when
        client.execute();

        // then
        // log.info("result = {}", result);
    }

    @Test
    void decoratorTest1() {
        // given
        Component realComponent = new RealComponent();
        Component messageDecorator = new MessageDecorator(realComponent);
        DecoratorPatternClient client = new DecoratorPatternClient(messageDecorator);

        // when
        client.execute();

        // then
        // log.info("result = {}", result);
    }
}
