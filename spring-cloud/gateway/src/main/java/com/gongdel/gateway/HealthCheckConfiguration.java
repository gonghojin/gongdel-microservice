package com.gongdel.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthIndicator;
import org.springframework.boot.actuate.health.DefaultReactiveHealthIndicatorRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;

@Configuration
public class HealthCheckConfiguration {

	private final WebClient.Builder webClientBuilder;
	private final HealthAggregator healthAggregator;
	private WebClient webClient;

	@Autowired
	public HealthCheckConfiguration(
			WebClient.Builder webClientBuilder,
			HealthAggregator healthAggregator
	) {
		this.webClientBuilder = webClientBuilder;
		this.healthAggregator = healthAggregator;
	}

	@Bean
	ReactiveHealthIndicator healthIndicatorMicroservices() {
		ReactiveHealthIndicatorRegistry registry =
				new DefaultReactiveHealthIndicatorRegistry(new LinkedHashMap<>());

		registry.register("auth-server",() -> getHealth("http://auth-server"));
		registry.register("product", () -> getHealth("http://product"));
		registry.register("recommendation", () -> getHealth("http://recommendation"));
		registry.register("review", () -> getHealth("http://review"));
		registry.register("product-composite", () -> getHealth("http://product-composite"));

		return new CompositeReactiveHealthIndicator(healthAggregator, registry);
	}

	private Mono<Health> getHealth(String url) {
		url += "/actuator/health";

		return getWebClient().get().uri(url)
				.retrieve().bodyToMono(String.class)
				.map(s -> new Health.Builder().up().build())
				.onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
				.log();
	}

	private WebClient getWebClient() {
		if (webClient == null) {
			webClient = webClientBuilder.build();
		}
		return webClient;
	}
}
