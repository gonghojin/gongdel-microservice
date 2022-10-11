package com.gongdel.microservices.composite.product;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import static org.springframework.http.HttpMethod.*;

@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
		httpSecurity
				.authorizeExchange()
				.pathMatchers("/actuator/**").permitAll()
				.pathMatchers(POST, "/product-composite/**").hasAuthority("SCOPE_product:write") // 스프링 시큐리티로 권한을 검사하는 경우, 스코프 이름 앞에 SCOPE_ 가 관례
				.pathMatchers(DELETE, "/product-composite/**").hasAuthority("SCOPE_product:write")
				.pathMatchers(GET, "/product-composite/**").hasAuthority("SCOPE_product:read")
				.anyExchange().authenticated()
				.and()
				.oauth2ResourceServer()
				.jwt();

		return httpSecurity.build();
	}
}
