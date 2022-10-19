package com.gongdel.microservices.api.core.prduct;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

public interface ProductService {

	Product createProduct(@RequestBody Product body);

	@GetMapping(
			value = "/product/{productId}",
			produces = "application/json")
	Mono<Product> getProduct(
			@PathVariable int productId,
			@RequestParam(value = "delay", required = false, defaultValue = "0") int delay, // 매개변수에 따라 응답 지연
			@RequestParam(value = "faultPercent", required = false, defaultValue = "0") int faultPercent // 백분율에 따른무작위 예외 발생
	);

	void deleteProduct(@PathVariable int productId);
}
