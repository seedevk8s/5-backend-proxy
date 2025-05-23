package com.choongang.proxy;

import com.choongang.proxy.config.AppV2Config;
import com.choongang.proxy.config.v1_proxy.InterfaceProxyConfig;
import com.choongang.proxy.trace.logtrace.LogTrace;
import com.choongang.proxy.trace.logtrace.ThreadLocalLogTrace;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Arrays;

//@Import(AppV2Config.class)
@Import(InterfaceProxyConfig.class)
@SpringBootApplication(scanBasePackages = {"com.choongang.proxy.app"})
public class ProxyApplication {

    public static void main(String[] args) {
        System.out.println("=== Spring Boot 애플리케이션 시작 ===");

        // Spring Boot 애플리케이션 컨텍스트 생성 및 시작
        SpringApplication.run(ProxyApplication.class, args);

        System.out.println("=== 애플리케이션 컨텍스트 초기화 완료 ===");
    }

    // LogTrace를 ThreadLocalLogTrace로 설정하여 스레드마다 독립적인 로그 추적기를 사용하도록 설정
    @Bean
    public LogTrace logTrace() {
        return new ThreadLocalLogTrace();
    }

    // 애플리케이션 시작 후 자동 실행됨
    @Bean
    public CommandLineRunner printBeans(ApplicationContext ctx) {
        return args -> {
            System.out.println("== 등록된 Bean 목록 ==");
            Arrays.stream(ctx.getBeanDefinitionNames())
                    .filter(name -> name.contains("order") || name.contains("v2") || name.contains("controller") || name.contains("service") || name.contains("logTrace"))
                    .sorted()
                    .forEach(System.out::println);
        };
    }    // CommandLineRunner를 사용하여 애플리케이션 시작 시 실행되는 메서드, 등록된 Bean 목록을 출력하는 메서드


}
