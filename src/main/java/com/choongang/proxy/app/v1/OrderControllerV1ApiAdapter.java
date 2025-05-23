package com.choongang.proxy.app.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 별도의 REST 컨트롤러를 만들어 프록시 객체를 주입받아 사용
@RestController
public class OrderControllerV1ApiAdapter {

    // Spring이 OrderControllerV1 타입의 빈을 찾아서 주입
    // InterfaceProxyConfig에서 생성한 OrderControllerInterfaceProxy 인스턴스가 주입됨 (이걸 사용한 이유: 프록시 객체에 추가적인 로그 기능을 넣기 위해)
    private final OrderControllerV1 orderController;

    // 생성자 주입
    public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
        this.orderController = orderController; // 실제로는 프록시 객체가 주입됨
    }

    @GetMapping("/v1/request")
    public String request(@RequestParam("itemId") String itemId) {
        // 여기서 실제 메서드 호출이 발생
        return orderController.request(itemId); // 프록시의 request() 메서드 호출
        //     ↑ 이 부분이 시퀀스 다이어그램의 화살표에 해당
    }
}

