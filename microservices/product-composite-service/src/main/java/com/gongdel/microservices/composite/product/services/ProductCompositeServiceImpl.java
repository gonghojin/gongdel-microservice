package com.gongdel.microservices.composite.product.services;

import com.gongdel.microservices.api.composite.product.ProductAggregate;
import com.gongdel.microservices.api.composite.product.ProductCompositeService;
import com.gongdel.microservices.api.composite.product.RecommendationSummary;
import com.gongdel.microservices.api.composite.product.ReviewSummary;
import com.gongdel.microservices.api.composite.product.ServiceAddresses;
import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.api.core.recommendation.Recommendation;
import com.gongdel.microservices.api.core.review.Review;
import com.gongdel.util.http.ServiceUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class ProductCompositeServiceImpl implements ProductCompositeService {

	private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeServiceImpl.class);

	private final ProductCompositeIntegration integration;
	private final ServiceUtil serviceUtil;

	// 생성 이벤트 publish
	@Override
	public void createCompositeProduct(ProductAggregate body) {
		try {
			LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.getProductId());

			Product product = new Product(body.getProductId(), body.getName(), body.getWeight(), null);
			integration.createProduct(product);

			if (body.getRecommendations() != null) {
				body.getRecommendations()
						.forEach(r -> {
							Recommendation recommendation = new Recommendation(body.getProductId(),
									r.getRecommendationId(), r.getAuthor(), r.getRate(), r.getContent(), null);
							integration.createRecommendation(recommendation);
						});
			}

			if (body.getReviews() != null) {
				body.getReviews()
						.forEach(r -> {
							Review review = new Review(body.getProductId(), r.getReviewId(), r.getAuthor(),
									r.getSubject(),
									r.getContent(), null);
							integration.createReview(review);
						});
			}
			LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.getProductId());

		} catch (RuntimeException re) {
			LOG.warn("createCompositeProduct failed: {}", re.toString());
			throw re;
		}
	}

	@Override
	public Mono<ProductAggregate> getCompositeProduct(int productId) {
		// 병렬 실행 후 Aggregate
		return Mono.zip(
				values -> createProductAggregate(
						(Product) values[0],
						(List<Recommendation>) values[1],
						(List<Review>) values[2],
						serviceUtil.getServiceAddress()
				),
				integration.getProduct(productId), // 해당 API 실패 시, 전체 요청이 실패
				// 두 API는 예외를 전파하는 대신 가능한 많은 정보를 호출자에게 돌려주기 위해, onErrorResume을 사용하여 빈 오프젝트 반환
				integration.getRecommendations(productId).collectList(),
				integration.getReviews(productId).collectList())
				.doOnError(ex -> LOG.warn("getCompositeProduct failed: {}", ex.toString()))
				.log();
	}

	private ProductAggregate createProductAggregate(Product product, List<Recommendation> recommendations,
													List<Review> reviews, String serviceAddress) {


		int productId = product.getProductId();
		String name = product.getName();
		int weight = product.getWeight();

		List<RecommendationSummary> recommendationSummaries = getRecommendationSummaries(recommendations);
		List<ReviewSummary> reviewSummaries = getReviewSummaries(reviews);

		String productAddress = product.getServiceAddress();
		String reviewAddress = (reviews != null && reviews.size() > 0) ? reviews.get(0).getServiceAddress() : "";
		String recommendationAddress = (recommendations != null && recommendations.size() > 0) ?
				recommendations.get(0).getServiceAddress() : "";
		ServiceAddresses serviceAddresses = new ServiceAddresses(serviceAddress, productAddress, reviewAddress,
				recommendationAddress);

		return new ProductAggregate(productId, name, weight, recommendationSummaries, reviewSummaries,
				serviceAddresses);
	}

	private List<RecommendationSummary> getRecommendationSummaries(List<Recommendation> recommendations) {
		return (recommendations == null) ? null :
				recommendations.stream()
						.map(r -> new RecommendationSummary(r.getRecommendationId(), r.getAuthor(), r.getRate(),
								r.getContent()))
						.collect(Collectors.toList());
	}

	private List<ReviewSummary> getReviewSummaries(List<Review> reviews) {
		return (reviews == null) ? null :
				reviews.stream()
						.map(r -> new ReviewSummary(r.getReviewId(), r.getAuthor(), r.getSubject(), r.getContent()))
						.collect(Collectors.toList());
	}

	@Override
	public void deleteCompositeProduct(int productId) {

	}
}
