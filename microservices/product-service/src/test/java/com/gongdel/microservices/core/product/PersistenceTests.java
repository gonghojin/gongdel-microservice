package com.gongdel.microservices.core.product;

import com.gongdel.microservices.core.product.persistence.ProductEntity;
import com.gongdel.microservices.core.product.persistence.ProductRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.test.StepVerifier;

@RunWith(SpringRunner.class)
@DataMongoTest
public class PersistenceTests {

	@Autowired
	private ProductRepository repository;

	private ProductEntity savedEntity;

	@Before
	public void setupDb() {
		StepVerifier.create(repository.deleteAll()).verifyComplete();

		ProductEntity entity = new ProductEntity(1, "n", 1);
		StepVerifier.create(repository.save(entity))
				.expectNextMatches(createdEntity -> {
					savedEntity = createdEntity;
					return areProductEqual(entity, savedEntity);
				})
				.verifyComplete();
	}

	@Test
	public void create() {
		// Given
		ProductEntity newEntity = new ProductEntity(2, "n", 2);

		// When
		// Then
		StepVerifier.create(repository.save(newEntity))
				.expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())
				.verifyComplete();

		StepVerifier.create(repository.findById(newEntity.getId()))
				.expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
				.verifyComplete();

		StepVerifier.create(repository.count()).expectNext(2l).verifyComplete();
	}

	private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
		return
				(expectedEntity.getId().equals(actualEntity.getId())) &&
						(expectedEntity.getVersion() == actualEntity.getVersion()) &&
						(expectedEntity.getProductId() == actualEntity.getProductId()) &&
						(expectedEntity.getName().equals(actualEntity.getName())) &&
						(expectedEntity.getWeight() == actualEntity.getWeight());
	}
}
