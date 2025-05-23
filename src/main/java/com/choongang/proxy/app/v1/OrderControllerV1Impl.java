package com.choongang.proxy.app.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
@RequiredArgsConstructor
public class OrderControllerV1Impl implements OrderControllerV1 {

    private final OrderServiceV1 orderService;

    /**
     * 주문 요청
     * @param itemId 상품 아이디
     * @return 결과
     */
    @GetMapping("/v1/request")
    @Override
    public String request(String itemId) {
        orderService.orderItem(itemId);
        return "ok";
    }
}
