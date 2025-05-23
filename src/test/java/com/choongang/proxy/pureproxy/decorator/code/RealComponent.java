package com.choongang.proxy.pureproxy.decorator.code;

import lombok.extern.slf4j.Slf4j;

/**
 * 실제 객체
 * Decorator 패턴에서 Decorator가 감싸는 대상이 되는 객체
 * Component 인터페이스를 구현한다.
 */
@Slf4j
public class RealComponent implements Component {

    @Override
    public String operation() {
        log.info("RealComponent 실행");
        return "data"; // 실제 데이터 반환
    }
}
