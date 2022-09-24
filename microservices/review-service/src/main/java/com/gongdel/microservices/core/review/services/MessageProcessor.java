package com.gongdel.microservices.core.review.services;

import com.gongdel.microservices.api.core.review.Review;
import com.gongdel.microservices.api.core.review.ReviewService;
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
	private ReviewService reviewService;

	@StreamListener(target = Sink.INPUT)
	public void process(Event<Integer, Review> event) {
		LOG.info("Process message created at {}...", event.getEventCreatedAt());

		switch (event.getEventType()) {
			case CREATE:
				Review review = event.getData();
				LOG.info("Create review with ID: {}/{}", review.getProductId(), review.getReviewId());
				reviewService.createReview(review);

				break;

			case DELETE:
				int productId = event.getKey();
				LOG.info("Delete reviews with ProductID: {}", productId);
				reviewService.deleteReviews(productId);

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