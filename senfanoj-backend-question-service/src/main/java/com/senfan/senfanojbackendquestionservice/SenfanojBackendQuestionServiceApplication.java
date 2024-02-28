package com.senfan.senfanojbackendquestionservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.senfan.senfanojbackendquestionservice.mapper")
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.senfan")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.senfan.senfanojbackendserviceclient.service"})
public class SenfanojBackendQuestionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SenfanojBackendQuestionServiceApplication.class, args);
    }

}
