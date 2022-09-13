package com.gongdel.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.prduct.ProductService;
import com.gongdel.microservices.api.core.recommendation.Recommendation;
import com.gongdel.microservices.api.core.recommendation.RecommendationService;
import com.gongdel.microservices.api.core.review.Review;
import com.gongdel.microservices.api.core.review.ReviewService;
import com.gongdel.util.exceptions.InvalidInputException;
import com.gongdel.util.exceptions.NotFoundException;
import com.gongdel.util.http.HttpErrorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

import static reactor.core.publisher.Flux.empty;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

	private final WebClient webClient;
	private final ObjectMapper mapper;

	private final String productServiceUrl;
	private final String recommendationServiceUrl;
	private final String reviewServiceUrl;

	@Autowired
	public ProductCompositeIntegration(
			WebClient.Builder webClient,
			ObjectMapper mapper,
			@Value("${app.product-service.host}") String productServiceHost,
			@Value("${app.product-service.port}") int productServicePort,

			@Value("${app.recommendation-service.host}") String recommendationServiceHost,
			@Value("${app.recommendation-service.port}") int recommendationServicePort,

			@Value("${app.review-service.host}") String reviewServiceHost,
			@Value("${app.review-service.port}") int reviewServicePort
	) {
		this.webClient = webClient.build();
		this.mapper = mapper;

		this.productServiceUrl = "http://" + productServiceHost + ":" + productServicePort;
		this.recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort;
		this.reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort;
	}

	@Override
	public Product createProduct(Product body) {
		return null;
	}

	@Override
	public Mono<Product> getProduct(int productId) {
		String url = productServiceUrl + "/product/" + productId;
		LOG.debug("Will call the getProduct API on URL: {}", url);

		return webClient.get().uri(url)
				.retrieve()
				.bodyToMono(Product.class)
				.log()
				.onErrorMap(WebClientResponseException.class,
						e -> handleException(e)
				);
	}

	@Override
	public void deleteProduct(int productId) {

	}

	@Override
	public Recommendation createRecommendation(Recommendation body) {
		return null;
	}

	@Override
	public Flux<Recommendation> getRecommendations(int productId) {
		String url = recommendationServiceUrl + "/recommendation?productId=" + productId;
		LOG.debug("Will call the getRecommendations API on URL: {}", url);

		return webClient.get().uri(url)
				.retrieve()
				.bodyToFlux(Recommendation.class)
				.log()
				.onErrorResume(error -> empty());
	}

	@Override
	public void deleteRecommendations(int productId) {

	}

	@Override
	public Review createReview(Review body) {
		return null;
	}

	@Override
	public Flux<Review> getReviews(int productId) {
		String url = reviewServiceUrl + "/review?productId=" + productId;
		LOG.debug("Will call the getReviews API on URL: {}", url);

		return webClient.get().uri(url)
				.retrieve()
				.bodyToFlux(Review.class)
				.onErrorResume(error -> empty());
	}

	@Override
	public void deleteReviews(int productId) {

	}


	private Throwable handleException(Throwable ex) {
		if (!(ex instanceof WebClientResponseException)) {
			LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString());
			return ex;
		}

		WebClientResponseException wcre = (WebClientResponseException) ex;

		switch (wcre.getStatusCode()) {
			case NOT_FOUND:
				return new NotFoundException(getErrorMessage(wcre));

			case UNPROCESSABLE_ENTITY:
				return new InvalidInputException(getErrorMessage(wcre));

			default:
				LOG.warn("Got a unexpected HTTP error: {}, will rethrow it", wcre.getStatusCode());
				LOG.warn("Error body: {}", wcre.getResponseBodyAsString());
				return ex;
		}
	}

	private String getErrorMessage(WebClientResponseException ex) {
		try {
			return mapper.readValue(ex.getResponseBodyAsString(), HttpErrorInfo.class).getMessage();
		} catch (IOException ioex) {
			return ex.getMessage();
		}
	}
}
