package com.choongang.proxy.trace.templatecallback;

import com.choongang.proxy.trace.TraceStatus;
import com.choongang.proxy.trace.logtrace.LogTrace;

public class TraceTemplate {

    private final LogTrace trace;

    public TraceTemplate(LogTrace trace) {
        this.trace = trace;
    }

    public <T> T execute(String message, TraceCallback<T> callback) {
        TraceStatus status = null;
        try {
            status = trace.begin(message);
            // 비즈니스 로직 실행
            T result = callback.call(); // 콜백 메서드 호출
            trace.end(status);
            return result;
        } catch (Exception e) {
            trace.exception(status, e);
            throw e; // 예외를 다시 던짐
        }
    }
}
