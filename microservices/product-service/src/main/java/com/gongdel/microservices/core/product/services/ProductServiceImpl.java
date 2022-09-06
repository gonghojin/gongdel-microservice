package com.gongdel.microservices.core.product.services;

import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.prduct.ProductService;
import com.gongdel.microservices.core.product.persistence.ProductEntity;
import com.gongdel.microservices.core.product.persistence.ProductRepository;
import com.gongdel.util.exceptions.InvalidInputException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

	private final ProductRepository repository;
	private final ProductMapper mapper;

	@Override
	public Product createProduct(Product body) {
		if (body.getProductId() < 1) {
			throw new InvalidInputException("Invalid productId: " + body.getProductId());
		}

		ProductEntity entity = mapper.apiToEntity(body);
		Mono<Product> newEntity = repository.save(entity)
				.log()
				.onErrorMap(
						DuplicateKeyException.class,
						ex -> new InvalidInputException("Duplicate key, Product Id: " + body.getProductId())
				)
				.map(e -> mapper.entityToApi(e));
		return newEntity.block();

	}

	@Override
	public Mono<Product> getProduct(int productid) {
		return null;
	}

	@Override
	public void deleteProduct(int productId) {

	}
}