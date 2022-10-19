package com.gongdel.microservices.core.product.services;

import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.prduct.ProductService;
import com.gongdel.microservices.core.product.persistence.ProductEntity;
import com.gongdel.microservices.core.product.persistence.ProductRepository;
import com.gongdel.util.exceptions.InvalidInputException;
import com.gongdel.util.exceptions.NotFoundException;
import com.gongdel.util.http.ServiceUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Random;

@RestController
@Slf4j
public class ProductServiceImpl implements ProductService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductServiceImpl.class);

	private final ProductRepository repository;
	private final ProductMapper mapper;
	private final ServiceUtil serviceUtil;

	private final Random randomNumberGenerator = new Random();

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
	public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		if (delay > 0) simulateDelay(delay);
		if (faultPercent > 0) throwErrorIfBadLuck(faultPercent);

		return repository.findByProductId(productId)
				.switchIfEmpty(Mono.error(new NotFoundException("No product found for productId: " + productId)))
				.log()
				.map(e -> mapper.entityToApi(e))
				.map(e -> {
					e.setServiceAddress(serviceUtil.getServiceAddress());
					return e;
				});
	}

	private void simulateDelay(int delay) {
		LOG.debug("Sleeping for {} seconds...", delay);
		try {
			Thread.sleep(delay * 1000);
		} catch (InterruptedException e) {
		}
		LOG.debug("Moving on...");
	}

	// 임의로 생성한 1에서 100 사이의 수가 지정된 오류 백분율보다 크거나 같으면 예외 발생
	private void throwErrorIfBadLuck(int faultPercent) {
		int randomThreshold = getRandomNumber(1, 100);
		if (faultPercent < randomThreshold) {
			LOG.debug("We got lucky, no error occurred, {} < {}", faultPercent, randomThreshold);
		} else {
			LOG.debug("Bad luck, an error occurred, {} >= {}", faultPercent, randomThreshold);
			throw new RuntimeException("Something went wrong...");
		}
	}


	private int getRandomNumber(int min, int max) {
		if (max < min) {
			throw new RuntimeException("Max must be greater than min");
		}

		return randomNumberGenerator.nextInt((max - min) + 1) + min;
	}

	@Override
	public void deleteProduct(int productId) {
		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}

		LOG.debug("deleteProduct: tries to delete an entity with productId: {}", productId);
		repository.findByProductId(productId).log()
				.map(e -> repository.delete(e))
				.flatMap(e -> e).block();
		// https://velog.io/@backtony/Spring-WebFlux-%EB%A6%AC%EC%95%A1%ED%84%B0-%ED%83%80%EC%9E%85-%EB%A6%AC%ED%8F%AC%EC%A7%80%ED%86%A0%EB%A6%AC-%ED%85%8C%EC%8A%A4%ED%8A%B8
	}
}