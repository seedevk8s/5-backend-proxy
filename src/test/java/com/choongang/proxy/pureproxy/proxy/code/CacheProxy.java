package com.choongang.proxy.pureproxy.proxy.code;

import lombok.extern.slf4j.Slf4j;

/**
 * 캐시 프록시
 * 캐시된 데이터가 없으면 실제 객체를 호출하고, 있으면 캐시된 데이터를 반환한다.
 */
@Slf4j
public class CacheProxy implements Subject{

    private Subject target;     // 실제 객체
    private String cacheValue; // 캐시된 데이터

    public CacheProxy(Subject target) {     // 프록시 객체가 실제 객체 주입받음.  프록시 객체가 실제 객체를 사용한다
        this.target = target;
    }



    @Override
    public String operation() {
        log.info("CacheProxy operation 호출");
        log.info("target = {}", target.getClass());
        log.info("cacheValue = {}", cacheValue);

        if (cacheValue == null) {    // 캐시가 비어있으면
            log.info("캐시가 비어있음. 실제 객체 호출");
            cacheValue = target.operation();   // 실제 객체 호출
        } else {
            log.info("캐시에서 데이터 조회");
        }
        return cacheValue;
    }
}
