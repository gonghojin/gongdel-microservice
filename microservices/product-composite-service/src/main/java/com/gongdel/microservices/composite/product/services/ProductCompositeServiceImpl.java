package com.gongdel.microservices.composite.product.services;

import com.gongdel.microservices.api.composite.product.ProductAggregate;
import com.gongdel.microservices.api.composite.product.ProductCompositeService;
import com.gongdel.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class ProductCompositeServiceImpl implements ProductCompositeService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

	private final ProductCompositeIntegration integration;
	private final ServiceUtil serviceUtil;

	@Override
	public void createCompositeProduct(ProductAggregate body) {

	}

	@Override
	public Mono<ProductAggregate> getCompositeProduct(int productId) {
		return null;
	}

	@Override
	public void deleteCompositeProduct(int productId) {

	}
}
