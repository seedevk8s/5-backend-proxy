package com.choongang.proxy.config.v1_proxy;

import com.choongang.proxy.app.v1.*;
import com.choongang.proxy.config.v1_proxy.interface_proxy.OrderControllerInterfaceProxy;
import com.choongang.proxy.config.v1_proxy.interface_proxy.OrderRepositoryInterfaceProxy;
import com.choongang.proxy.config.v1_proxy.interface_proxy.OrderServiceInterfaceProxy;
import com.choongang.proxy.trace.logtrace.LogTrace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InterfaceProxyConfig {
     @Bean
     public OrderControllerV1 orderController(LogTrace logTrace) {
         OrderControllerV1 orderControllerV1 = new OrderControllerV1Impl(orderService(logTrace));
         return new OrderControllerInterfaceProxy(orderControllerV1, logTrace);
     }

     @Bean
     public OrderServiceV1 orderService(LogTrace logTrace) {
         OrderServiceV1 orderServiceV1 = new OrderServiceV1Impl(orderRepository(logTrace));
         return new OrderServiceInterfaceProxy(orderServiceV1, logTrace);
     }

     @Bean
     public OrderRepositoryV1 orderRepository(LogTrace logTrace) {
         OrderRepositoryV1 orderRepositoryV1 = new OrderRepositoryV1Impl();
         return new OrderRepositoryInterfaceProxy(orderRepositoryV1, logTrace);
     }
}
