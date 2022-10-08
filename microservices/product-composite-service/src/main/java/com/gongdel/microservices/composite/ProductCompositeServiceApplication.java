package com.gongdel.microservices.composite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@ComponentScan("com.gongdel")
public class ProductCompositeServiceApplication {

	@Bean
	@LoadBalanced // 로드밸런서 관련 필터를 해당 빈에 주입, 통합 클래스에서 생성자가 실행될 떄까지 수행되지 않으므로, 별도의 getter 메서드를 두고 늦은 초기화 방식 필요
	public WebClient.Builder loadBalancedWebClientBuilder() {
		final WebClient.Builder builder = WebClient.builder();
		return builder;
	}

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
	}

}
