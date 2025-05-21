package com.choongang.proxy;

import com.choongang.proxy.config.AppV2Config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

@Import(AppV2Config.class)
@SpringBootApplication(scanBasePackages = {"com.choongang.proxy.app"})
public class ProxyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }

    // 애플리케이션 시작 후 자동 실행됨
    @Bean
    public CommandLineRunner printBeans(ApplicationContext ctx) {
        return args -> {
            System.out.println("== 등록된 Bean 목록 ==");
            Arrays.stream(ctx.getBeanDefinitionNames())
                    .filter(name -> name.contains("order") || name.contains("v2") || name.contains("controller"))
                    .sorted()
                    .forEach(System.out::println);
        };
    }
}
