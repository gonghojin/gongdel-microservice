package com.gongdel.microservices.core.product.services;

import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.prduct.ProductService;
import com.gongdel.microservices.api.event.Event;
import com.gongdel.util.exceptions.EventProcessingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

// 하나의 토픽만 수신
@EnableBinding(Sink.class)
@RequiredArgsConstructor
public class MessageProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);

	private final ProductService productService;

	@StreamListener(target = Sink.INPUT)
	public void process(Event<Integer, Product> event) {
		LOG.info("Process message created at {}...", event.getEventCreatedAt());

		switch (event.getEventType()) {
			case CREATE:
				Product product = event.getData();
				LOG.info("Create product with ID: {}", product.getProductId());
				productService.createProduct(product);

				break;

			case DELETE:
				int productId = event.getKey();
				LOG.info("Delete recommendations with ProductID: {}", productId);
				productService.deleteProduct(productId);

				break;

			default:
				String errorMessage = String.format("Incorrect event type: %s,  expected a CREATE or DELETE event",
						event.getEventType());
				LOG.warn(errorMessage);
				throw new EventProcessingException(errorMessage);
		}

		LOG.info("Message processing done!");
	}
}
