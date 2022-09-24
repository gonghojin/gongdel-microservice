package com.gongdel.microservices.core.product.services;

import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.prduct.ProductService;
import com.gongdel.microservices.core.product.persistence.ProductEntity;
import com.gongdel.microservices.core.product.persistence.ProductRepository;
import com.gongdel.util.exceptions.InvalidInputException;
import com.gongdel.util.exceptions.NotFoundException;
import com.gongdel.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {

	private final ProductRepository repository;
	private final ProductMapper mapper;
	private final ServiceUtil serviceUtil;

	@Autowired
	public ProductServiceImpl(ProductRepository repository, ProductMapper mapper, ServiceUtil serviceUtil) {
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
	}

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

		// MessageProcessor 는 블로킹 프로그래밍 모델을 기반으로 하므로, block 메서드를 호출한 후에 결과 반환
		// block() 메서드를 호출하지 않으면, 메시징 시스템이 서비스 구현에서 발생한 오류를 처리하지 못하므로,
		// 이벤트가 대기열로 다시 들어가지 못하고, 데드 레터 대기열로 이동하게 된다
		return newEntity.block();

	}

	@Override
	public Mono<Product> getProduct(int productId) {
		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}
		;

		return repository.findByProductId(productId)
				.switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
				.log()
				.map(e -> mapper.entityToApi(e))
				.map(e -> {
					e.setServiceAddress(serviceUtil.getServiceAddress());
					return e;
				});
	}

	@Override
	public void deleteProduct(int productId) {

	}
}