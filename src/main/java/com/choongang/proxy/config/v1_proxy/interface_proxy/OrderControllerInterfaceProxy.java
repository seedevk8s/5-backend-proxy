package com.choongang.proxy.config.v1_proxy.interface_proxy;

import com.choongang.proxy.app.v1.OrderControllerV1;
import com.choongang.proxy.trace.TraceStatus;
import com.choongang.proxy.trace.logtrace.LogTrace;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrderControllerInterfaceProxy implements OrderControllerV1 {

    private final OrderControllerV1 target; // 실제 객체
    private final LogTrace logTrace; // 로그 추적기

    /**
     * 프록시 객체가 실제 객체를 주입받음.  프록시 객체가 실제 객체를 사용한다
     * @param orderControllerV1
     * @param logTrace
     */
    @Override
    public String request(String itemId) {
        //log.info("OrderController.request() 호출");
        //log.info("target = {}", target.getClass());
        //log.info("itemId = {}", itemId);

        // 로그 시작
        TraceStatus status = null;
        try {
            status = logTrace.begin("OrderController.request()");
            String result = target.request(itemId); // 실제 객체 호출
            logTrace.end(status); // 로그 종료
            return result;
        } catch (Exception e) {
            logTrace.exception(status, e); // 예외 발생 시 로그 처리
            throw e; // 예외를 다시 던짐
        }
    }
}
