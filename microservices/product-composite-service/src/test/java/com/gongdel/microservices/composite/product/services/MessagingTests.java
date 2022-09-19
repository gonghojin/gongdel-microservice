package com.gongdel.microservices.composite.product.services;

import com.gongdel.microservices.api.composite.product.ProductAggregate;
import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.event.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.concurrent.BlockingQueue;

import static com.gongdel.microservices.api.event.Event.Type.CREATE;
import static com.gongdel.microservices.api.event.Event.Type.DELETE;
import static com.gongdel.microservices.composite.product.services.IsSameEvent.sameEventExceptCreatedAt;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.stream.test.matcher.MessageQueueMatcher.receivesPayloadThat;
import static org.springframework.http.HttpStatus.OK;
import static reactor.core.publisher.Mono.just;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class MessagingTests {

	@Autowired
	WebTestClient client;

	@Autowired
	MessageCollector collector;

	@Autowired
	ProductCompositeIntegration.MessageSources channels;

	BlockingQueue<Message<?>> queueProducts = null;
	BlockingQueue<Message<?>> queueRecommendations = null;
	BlockingQueue<Message<?>> queueReviews = null;

	@Before
	public void setUp() throws Exception {
		queueProducts = getQueue(channels.outputProducts());
		queueRecommendations = getQueue(channels.outputRecommendations());
		queueReviews = getQueue(channels.outputReviews());
	}

	@Test
	public void createCompositeProduct1() {
		ProductAggregate composite = new ProductAggregate(1, "name", 1, null, null, null);
		postAndVerifyProduct(composite, OK);

		// 큐에 한 가지 이벤트 저장
		assertEquals(1, queueProducts.size());

		Product product = new Product(composite.getProductId(), composite.getName(), composite.getWeight(), null);
		Event<Integer, Product> expectedEvent = new Event(CREATE, composite.getProductId(), product);
		assertThat(queueProducts, is(receivesPayloadThat(sameEventExceptCreatedAt(expectedEvent))));

		assertEquals(0, queueRecommendations.size());
		assertEquals(0, queueReviews.size());
	}

	private void postAndVerifyProduct(ProductAggregate composite, HttpStatus ok) {
		client.post()
				.uri("product-composite")
				.body(just(composite), ProductAggregate.class)
				.exchange()
				.expectStatus().isEqualTo(ok);
	}

	private BlockingQueue<Message<?>> getQueue(MessageChannel messageChannel) {
		return collector.forChannel(messageChannel);
	}


	@Test
	public void deleteCompositeProduct() {
		deleteAndVerifyProduct(1, OK);
		assertEquals(1, queueProducts.size());

		Event<Integer, Product> expectedEvent = new Event(DELETE, 1, null);
		assertThat(queueProducts, is(receivesPayloadThat(sameEventExceptCreatedAt(expectedEvent))));

		assertEquals(1, queueRecommendations.size());
		Event<Integer, Product> expectedRecommendationEvent = new Event(DELETE, 1, null);
		assertThat(queueRecommendations, receivesPayloadThat(sameEventExceptCreatedAt(expectedRecommendationEvent)));

		assertEquals(1, queueReviews.size());
		Event<Integer, Product> expectedReviewEvent = new Event(DELETE, 1, null);
		assertThat(queueReviews, receivesPayloadThat(sameEventExceptCreatedAt(expectedReviewEvent)));
	}

	private void deleteAndVerifyProduct(int productId, HttpStatus ok) {
		client.delete()
				.uri("/product-composite/" + productId)
				.exchange()
				.expectStatus().isEqualTo(ok);
	}
}
