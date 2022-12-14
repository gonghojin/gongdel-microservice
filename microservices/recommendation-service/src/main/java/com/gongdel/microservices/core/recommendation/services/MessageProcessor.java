package com.gongdel.microservices.core.recommendation.services;

import com.gongdel.microservices.api.core.recommendation.Recommendation;
import com.gongdel.microservices.api.core.recommendation.RecommendationService;
import com.gongdel.microservices.api.event.Event;
import com.gongdel.util.exceptions.EventProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

@EnableBinding(Sink.class)
public class MessageProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(MessageProcessor.class);

	@Autowired
	private RecommendationService recommendationService;

	@StreamListener(target = Sink.INPUT)
	public void process(Event<Integer, Recommendation> event) {

		LOG.info("Process message created at {}...", event.getEventCreatedAt());

		switch (event.getEventType()) {
			case CREATE:
				Recommendation recommendation = event.getData();
				LOG.info("Create recommendation with ID: {}/{}", recommendation.getProductId(),
						recommendation.getRecommendationId());
				recommendationService.createRecommendation(recommendation);

				break;

			case DELETE:
				int productId = event.getKey();
				LOG.info("Delete recommendations with ProductID: {}", productId);
				recommendationService.deleteRecommendations(productId);

				break;

			default:
				String errorMessage = "Incorrect event type: " + event.getEventType() + ", expected a CREATE or " +
						"DELETE" +
						" event";
				LOG.warn(errorMessage);
				throw new EventProcessingException(errorMessage);
		}

		LOG.info("Message processing done!");
	}
}
