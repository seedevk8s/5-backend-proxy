package com.choongang.proxy.pureproxy.proxy;

import com.choongang.proxy.pureproxy.proxy.code.ProxyPatternClient;
import com.choongang.proxy.pureproxy.proxy.code.RealSubject;
import org.junit.jupiter.api.Test;

public class ProxyPatternTest {
    // ProxyPatternClient 에서 Proxy 를 사용하지 않고, RealSubject 를 직접 사용한다
    @Test
    void noProxyTest() {
        // RealSubject는 실제 비즈니스 로직을 수행하는 클래스임.
        // 이 객체는 프록시 없이 직접 생성되며, 이후 클라이언트에서 호출됩니다.
        RealSubject realSubject = new RealSubject();
        // ProxyPatternClient는 클라이언트 역할을 하며, 생성자에서 RealSubject 객체를 주입받습니다.
        // 이 클라이언트는 주입된 객체를 통해 작업을 수행합니다.
        ProxyPatternClient client = new ProxyPatternClient(realSubject);    //ProxyPatternClient에 실제 객체 주입
        client.execute();       // execute 메서드는 RealSubject의 메서드를 호출합니다.
        client.execute();       // 이 테스트에서는 client.execute()를 3번 호출하며, 각 호출마다 RealSubject의 작업(operation())이 수행됩니다.
        client.execute();       // 데이터를 조회하는데 1초가 소모되므로 총 3초의 시간이 걸린다.

    }
}
