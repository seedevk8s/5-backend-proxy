package com.choongang.proxy.pureproxy.proxy.code;

/**
 * Subject 인터페이스에 의존하고, Subject 를 호출하는 클라이언트 코드이다
 * execute() 를 실행하면 subject.operation() 를 호출한다.
 */
public class ProxyPatternClient {

    private Subject subject;

    public ProxyPatternClient(Subject subject) {
        this.subject = subject;
    }

    public void execute() {
        String result = subject.operation();
    }
}
