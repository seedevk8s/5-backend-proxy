package com.choongang.proxy.app.v1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// 별도의 REST 컨트롤러를 만들어 프록시 객체를 주입받아 사용
@RestController
public class OrderControllerV1ApiAdapter {

    private final OrderControllerV1 orderController; // 이 객체는 프록시임

    public OrderControllerV1ApiAdapter(OrderControllerV1 orderController) {
        this.orderController = orderController;
    }

    @GetMapping("/v1/request")
    public String request(String itemId) {
        return orderController.request(itemId);
    }
}

