package com.choongang.proxy.pureproxy.proxy.code;

import lombok.extern.slf4j.Slf4j;

/**
 * 캐시 프록시
 * 캐시된 데이터가 없으면 실제 객체를 호출하고, 있으면 캐시된 데이터를 반환한다.
 */
@Slf4j
public class CacheProxy implements Subject{

    private Subject target;     // 실제 객체. 프록시가 호출하는 대상
    private String cacheValue; // 캐시된 데이터

    public CacheProxy(Subject target) {     // 프록시 객체가 실제 객체 주입받음.  프록시 객체가 실제 객체를 사용한다
        this.target = target;
    }


    /**
     * 만약 cacheValue 에 값이 있으면 실제 객체를 전혀 호출하지 않고, 캐시 값을 그대로 반환한다.
     * 따라서 처음 조회 이후에는 캐시( cacheValue )에서 매우 빠르게 데이터를 조회할 수 있다.
     * @return
     */
    @Override
    public String operation() {
        log.info("Proxy(operation) 호출");
        //log.info("target = {}", target.getClass());
        //log.info("cacheValue = {}", cacheValue);

        if (cacheValue == null) {    // 캐시가 비어있으면
            log.info("캐시가 비어있음. 실제 객체 호출");
            cacheValue = target.operation();   // 실제 객체 호출
        } else {
            log.info("캐시에서 데이터 조회");
        }
        return cacheValue;
    }
}
