package com.senfan.senfanojbackendjudgeservice;

// import com.senfan.senfanojbackendjudgeservice.rabbitmq.InitRabbitMq;
// import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan("com.senfan")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.senfan.senfanojbackendserviceclient.service"})
public class SenfanojBackendJudgeServiceApplication {

    public static void main(String[] args) {
        // 初始化消息队列
        // InitRabbitMq.doInit();
        SpringApplication.run(SenfanojBackendJudgeServiceApplication.class, args);
    }

}
