package com.choongang.proxy.trace.template;

import com.choongang.proxy.trace.TraceStatus;
import com.choongang.proxy.trace.logtrace.LogTrace;

public abstract class AbstractTemplate<T> {

    private final LogTrace trace;

    // 생성자를 통해 LogTrace 객체를 주입받아 의존성을 설정합니다.
    // LogTrace는 로그 추적과 관련된 기능을 제공하며, 이를 통해 템플릿 메서드 패턴에서
    // 공통 로직과 비즈니스 로직의 실행 시간을 측정하거나 로그를 기록할 수 있습니다.
    public AbstractTemplate(LogTrace trace) {
        this.trace = trace;
    }

    // 템플릿 메서드 패턴을 사용하여 비즈니스 로직을 실행하는 메서드입니다.
    public T execute(String message) {
        TraceStatus status = null;
        try {
            // 로그 추적 시작
            status = trace.begin(message);
            // 비즈니스 로직 실행
            T result = call(); // 하위 클래스에서 구현한 메서드 호출
            // 로그 추적 종료
            trace.end(status);
            return result; // 비즈니스 로직의 결과를 반환합니다. 필요에 따라 수정할 수 있습니다.
        } catch (Exception e) {
            // 예외 발생 시 로그 추적 종료
            trace.exception(status, e);
            throw e; // 예외를 다시 던져서 상위 호출자에게 전달합니다.
        }
    }

    protected abstract T call();

}
