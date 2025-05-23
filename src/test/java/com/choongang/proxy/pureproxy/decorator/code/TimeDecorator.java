package com.choongang.proxy.pureproxy.decorator.code;

import lombok.extern.slf4j.Slf4j;

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
