package com.gongdel.microservices.core.review.services;

import com.gongdel.microservices.api.core.review.Review;
import com.gongdel.microservices.api.core.review.ReviewService;
import com.gongdel.microservices.core.review.persistence.ReviewEntity;
import com.gongdel.microservices.core.review.persistence.ReviewRepository;
import com.gongdel.util.exceptions.InvalidInputException;
import com.gongdel.util.http.ServiceUtil;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

@RestController
public class ReviewServiceImpl implements ReviewService {

	private static final Logger LOG = LoggerFactory.getLogger(ReviewServiceImpl.class);

	private ReviewRepository repository;
	private ReviewMapper mapper;
	private ServiceUtil serviceUtil;
	private Scheduler scheduler;

	@Autowired
	public ReviewServiceImpl(ReviewRepository repository, ReviewMapper mapper, ServiceUtil serviceUtil,
							 Scheduler scheduler) {
		this.repository = repository;
		this.mapper = mapper;
		this.serviceUtil = serviceUtil;
		this.scheduler = scheduler;
	}

	@Override
	public Review createReview(Review body) {
		if (body.getProductId() < 1) throw new InvalidInputException("Invalid productId: " + body.getProductId());

		try {
			ReviewEntity entity = mapper.apiToEntity(body);
			ReviewEntity newEntity = repository.save(entity);

			LOG.debug("createReview: created a review entity: {}/{}", body.getProductId(), body.getReviewId());
			return mapper.entityToApi(newEntity);

		} catch (DataIntegrityViolationException dive) {
			throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId() + ", Review Id:" + body.getReviewId());
		}
	}

	@Override
	public Flux<Review> getReviews(int productId) {
		if (productId < 1) {
			throw new InvalidInputException("Invalid productId: " + productId);
		}
		LOG.info("Will get reviews for product with id={}", productId);

		return asyncFlux(() -> Flux.fromIterable(getByProductId(productId))).log(null, Level.FINE);
	}

	protected List<Review> getByProductId(int productId) {

		List<ReviewEntity> entityList = repository.findByProductId(productId);
		List<Review> list = mapper.entityListToApiList(entityList);
		list.forEach(e -> e.setServiceAddress(serviceUtil.getServiceAddress()));

		LOG.debug("getReviews: response size: {}", list.size());

		return list;
	}

	private <T> Flux<T> asyncFlux(Supplier<Publisher<T>> publisherSupplier) {
		return Flux.defer(publisherSupplier).subscribeOn(scheduler);
	}

	@Override
	public void deleteReviews(int productId) {
		if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);

		LOG.debug("deleteReviews: tries to delete reviews for the product with productId: {}", productId);
		repository.deleteAll(repository.findByProductId(productId));
	}
}
