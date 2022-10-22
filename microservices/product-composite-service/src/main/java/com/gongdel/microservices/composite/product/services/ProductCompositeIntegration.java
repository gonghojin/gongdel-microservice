package com.gongdel.microservices.composite.product.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.prduct.ProductService;
import com.gongdel.microservices.api.core.recommendation.Recommendation;
import com.gongdel.microservices.api.core.recommendation.RecommendationService;
import com.gongdel.microservices.api.core.review.Review;
import com.gongdel.microservices.api.core.review.ReviewService;
import com.gongdel.microservices.api.event.Event;
import com.gongdel.util.exceptions.InvalidInputException;
import com.gongdel.util.exceptions.NotFoundException;
import com.gongdel.util.http.HttpErrorInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import static com.gongdel.microservices.api.event.Event.Type.CREATE;
import static com.gongdel.microservices.api.event.Event.Type.DELETE;
import static reactor.core.publisher.Flux.empty;

@EnableBinding(ProductCompositeIntegration.MessageSources.class)
@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);

	private final WebClient.Builder webClientBuilder;
	private WebClient webClient;

	private final ObjectMapper mapper;
	private final MessageSources messageSources;

	// 유레카 서버를 통해 아래의  dn 을 가지고, 로드밸런싱
	private final String productServiceUrl = "http://product";
	private final String recommendationServiceUrl = "http://recommendation";
	private final String reviewServiceUrl = "http://review";

	// 해당 시간을 초과할 시, 에러 발생
	private final int productServiceTimeoutSec;

	// 토픽별 채널 등록
	public interface MessageSources {

		String OUTPUT_PRODUCTS = "output-products";
		String OUTPUT_RECOMMENDATIONS = "output-recommendations";
		String OUTPUT_REVIEWS = "output-reviews";

		@Output(OUTPUT_PRODUCTS)
		MessageChannel outputProducts();

		@Output(OUTPUT_RECOMMENDATIONS)
		MessageChannel outputRecommendations();

		@Output(OUTPUT_REVIEWS)
		MessageChannel outputReviews();
	}

	@Autowired
	public ProductCompositeIntegration(
			WebClient.Builder webClientBuilder,
			ObjectMapper mapper,
			MessageSources messageSources,
     		@Value("${app.product-service.timeoutSec}") int productServiceTimeoutSec
	) {
		this.webClientBuilder = webClientBuilder;
		this.mapper = mapper;
		this.messageSources = messageSources;
		this.productServiceTimeoutSec = productServiceTimeoutSec;

	}

	@Override
	public Product createProduct(Product body) {
		messageSources.outputProducts()
				.send(MessageBuilder.withPayload(new Event(CREATE, body.getProductId(), body)).build());

		return body;
	}

	@CircuitBreaker(name = "product")
	@Override
	public Mono<Product> getProduct(int productId, int delay, int faultPercent) {
		URI url = UriComponentsBuilder
				.fromUriString(productServiceUrl + "/product/{productId}?delay={delay}&faultPercent={faultPercent}")
				.build(productId, delay, faultPercent);

		LOG.debug("Will call the getProduct API on URL: {}", url);

		return getWebClient().get().uri(url)
				.retrieve()
				.bodyToMono(Product.class)
				.log()
				.onErrorMap(WebClientResponseException.class,
						e -> handleException(e)
				).timeout(Duration.ofSeconds(productServiceTimeoutSec)); // 해당 시간 초과 시, 에러 발생하여 서킷 브레이커를 트리거
	}

	@Override
	public void deleteProduct(int productId) {
		messageSources.outputProducts()
				.send(MessageBuilder.withPayload(new Event(DELETE, productId, null)).build());
	}

	@Override
	public Recommendation createRecommendation(Recommendation body) {
		messageSources.outputRecommendations()
				.send(MessageBuilder.withPayload(new Event(CREATE, body.getProductId(), body)).build());
		return body;
	}

	@Override
	public Flux<Recommendation> getRecommendations(int productId) {
		String url = recommendationServiceUrl + "/recommendation?productId=" + productId;
		LOG.debug("Will call the getRecommendations API on URL: {}", url);

		return getWebClient().get().uri(url)
				.retrieve()
				.bodyToFlux(Recommendation.class)
				.log()
				.onErrorResume(error -> empty());
	}

	@Override
	public void deleteRecommendations(int productId) {
		messageSources.outputRecommendations()
				.send(MessageBuilder.withPayload(new Event(DELETE, productId, null)).build());
	}

	@Override
	public Review createReview(Review body) {
		messageSources.outputReviews()
				.send(MessageBuilder.withPayload(new Event(CREATE, body.getProductId(), body)).build());
		return body;
	}

	@Override
	public Flux<Review> getReviews(int productId) {
		String url = reviewServiceUrl + "/review?productId=" + productId;
		LOG.debug("Will call the getReviews API on URL: {}", url);

		return getWebClient().get().uri(url)
				.retrieve()
				.bodyToFlux(Review.class)
				.onErrorResume(error -> empty());
	}

	@Override
	public void deleteReviews(int productId) {
		messageSources.outputReviews()
				.send(MessageBuilder.withPayload(new Event(DELETE, productId, null)).build());
	}

	private WebClient getWebClient() {
		if (webClient == null) {
			webClient = webClientBuilder.build();
		}
		return webClient;
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

	public Mono<Health> getProductHealth() {
		return getHealth(productServiceUrl);
	}

	public Mono<Health> getRecommendationHealth() {
		return getHealth(recommendationServiceUrl);
	}

	public Mono<Health> getReviewHealth() {
		return getHealth(reviewServiceUrl);
	}

	// msa 에서 각 인스턴스의 상태를 체크
	private Mono<Health> getHealth(String url) {
		url += "/actuator/health";
		LOG.debug("Will call the Health API on URL: {}", url);

		return webClient.get().uri(url)
				.retrieve()
				.bodyToMono(String.class)
				.map(s -> new Health.Builder().up().build())
				.onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
				.log();
	}
}
