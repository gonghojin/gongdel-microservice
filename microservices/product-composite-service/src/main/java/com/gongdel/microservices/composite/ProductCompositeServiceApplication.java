package com.gongdel.microservices.composite;

import com.gongdel.microservices.composite.product.services.ProductCompositeIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.CompositeReactiveHealthIndicator;
import org.springframework.boot.actuate.health.DefaultReactiveHealthIndicatorRegistry;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.LinkedHashMap;

@SpringBootApplication
@ComponentScan("com.gongdel")
public class ProductCompositeServiceApplication {

	private HealthAggregator healthAggregator;
	private ProductCompositeIntegration integration;

	@Autowired
	public ProductCompositeServiceApplication(HealthAggregator healthAggregator,
											  ProductCompositeIntegration integration) {
		this.healthAggregator = healthAggregator;
		this.integration = integration;
	}

	public static void main(String[] args) {
		SpringApplication.run(ProductCompositeServiceApplication.class, args);
	}

	@Bean
	ReactiveHealthIndicator coreServices() {

		ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry(new LinkedHashMap<>());

		registry.register("product", () -> integration.getProductHealth());
		registry.register("recommendation", () -> integration.getRecommendationHealth());
		registry.register("review", () -> integration.getReviewHealth());

		return new CompositeReactiveHealthIndicator(healthAggregator, registry);
	}
}
