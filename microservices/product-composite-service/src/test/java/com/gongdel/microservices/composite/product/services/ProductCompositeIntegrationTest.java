package com.gongdel.microservices.composite.product.services;

import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.recommendation.Recommendation;
import com.gongdel.microservices.api.core.review.Review;
import com.gongdel.microservices.composite.product.ProductCompositeServiceApplication;
import com.gongdel.util.exceptions.InvalidInputException;
import com.gongdel.util.exceptions.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@RunWith(SpringRunner.class)
@SpringBootTest(
		webEnvironment = RANDOM_PORT,
		classes = {ProductCompositeServiceApplication.class, TestSecurityConfig.class},
		properties = {"spring.main.allow-bean-definition-overriding=true","eureka.client.enabled=false","spring.cloud.config.enabled=false"}// 등록된 bean을 오버라이딩 하기 위해서, default false
)
public class ProductCompositeIntegrationTest {

	private static final int PRODUCT_ID_OK = 1;
	private static final int PRODUCT_ID_NOT_FOUND = 2;
	private static final int PRODUCT_ID_INVALID = 3;

	@MockBean
	private ProductCompositeIntegration compositeIntegration;

	@Autowired
	private WebTestClient client;

	@Before
	public void setUp() {
		when(compositeIntegration.getProduct(eq(PRODUCT_ID_OK), anyInt(), anyInt()))
				.thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));

		when(compositeIntegration.getRecommendations(PRODUCT_ID_OK)).
				thenReturn(Flux.fromIterable(singletonList(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content"
						, "mock address"))));

		when(compositeIntegration.getReviews(PRODUCT_ID_OK)).
				thenReturn(Flux.fromIterable(singletonList(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content"
						, "mock address"))));

		when(compositeIntegration.getProduct(eq(PRODUCT_ID_NOT_FOUND), anyInt(), anyInt())).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

		when(compositeIntegration.getProduct(eq(PRODUCT_ID_INVALID), anyInt(), anyInt())).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
	}

	@Test
	public void getProductById() {
		getAndVerifyProduct(PRODUCT_ID_OK, OK)
				.jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
				.jsonPath("$.recommendations.length()").isEqualTo(1)
				.jsonPath("$.reviews.length()").isEqualTo(1);
	}

	private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
		return client.get()
				.uri("/product-composite/" + productId)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isEqualTo(expectedStatus)
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody();
	}
}
