package com.senfan.senfanojcodesandbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,RedisAutoConfiguration.class})
public class SenfanojCodeSandboxApplication {

	public static void main(String[] args) {
		SpringApplication.run(SenfanojCodeSandboxApplication.class, args);
	}

}
