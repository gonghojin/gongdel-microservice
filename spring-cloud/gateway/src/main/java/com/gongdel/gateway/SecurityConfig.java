package com.gongdel.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity httpSecurity) {
		httpSecurity
				.authorizeExchange()
				.pathMatchers("/actuator/**").permitAll() // 상용환경에서는  주의
				.anyExchange().authenticated()
				.and()
				.oauth2ResourceServer()
				.jwt(); // JWT로 인코딩된 Oauth 2.0 접근 토큰을 기반으로 인증 및 권한 부여

		return httpSecurity.build();
	}
}
