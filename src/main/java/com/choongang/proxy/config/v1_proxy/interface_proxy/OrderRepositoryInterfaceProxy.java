package com.choongang.proxy.config.v1_proxy.interface_proxy;

import com.choongang.proxy.app.v1.OrderRepositoryV1;
import com.choongang.proxy.trace.TraceStatus;
import com.choongang.proxy.trace.logtrace.LogTrace;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderRepositoryInterfaceProxy implements OrderRepositoryV1 {

    private final OrderRepositoryV1 target; // 실제 객체
    private final LogTrace logTrace; // 로그 추적기 (기능 추가)

    /**
     * 프록시 객체가 실제 객체를 주입받음.  프록시 객체가 실제 객체를 사용한다
     * @param orderRepositoryV1
     * @param logTrace
     */
    @Override
    public void save(String itemId) {

        TraceStatus status = null;
        try {
            status = logTrace.begin("OrderRepository.save()"); // 로그 시작
            target.save(itemId); // 실제 객체 호출
            logTrace.end(status); // 로그 종료
        } catch (Exception e) {
            logTrace.exception(status, e); // 예외 발생 시 로그 처리
            throw e; // 예외를 다시 던짐
        }
    }
}
