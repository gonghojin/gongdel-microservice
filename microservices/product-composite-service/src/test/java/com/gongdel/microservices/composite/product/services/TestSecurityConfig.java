package com.gongdel.microservices.composite.product.services;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@TestConfiguration
public class TestSecurityConfig {

	// 테스트 시, oauth는 비활성화 해야한다.
	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) { // 등록된 bean 을 오버라이딩하는 거기 때문에, 메소드 이름도 같아야함
		http.csrf().disable().authorizeExchange().anyExchange().permitAll();
		return http.build();
	}
}
