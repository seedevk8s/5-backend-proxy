package com.choongang.proxy.app.v1;

import org.springframework.web.bind.annotation.GetMapping;

public interface OrderControllerV1 {
    @GetMapping("/v1/request")
    String request(String itemId);
}
