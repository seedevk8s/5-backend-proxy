package com.choongang.proxy.pureproxy.proxy;

import com.choongang.proxy.pureproxy.proxy.code.ProxyPatternClient;
import com.choongang.proxy.pureproxy.proxy.code.RealSubject;
import org.junit.jupiter.api.Test;

public class ProxyPatternTest {

    @Test
    void noProxyTest() {
        // Client
        RealSubject realSubject = new RealSubject();
        ProxyPatternClient client = new ProxyPatternClient(realSubject);
        client.execute();       // 결과: 실제 객체 호출
        client.execute();
        client.execute();       // 테스트 코드에서는 client.execute() 를 3번 호출한다. 데이터를 조회하는데 1초가 소모되므로 총 3초의 시간이 걸린다.

    }
}
