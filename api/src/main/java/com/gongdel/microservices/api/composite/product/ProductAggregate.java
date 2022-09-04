package com.gongdel.microservices.api.composite.product;

import lombok.Getter;

import java.util.List;

@Getter
public class ProductAggregate {

	private final int productId;
	private final String name;
	private final int weight;
	private final List<RecommendationSummary> recommendations;
	private final List<ReviewSummary> reviews;
	private final ServiceAddresses serviceAddresses;

	public ProductAggregate() {
		productId = 0;
		name = null;
		weight = 0;
		recommendations = null;
		reviews = null;
		serviceAddresses = null;
	}

	public ProductAggregate(
			int productId,
			String name,
			int weight,
			List<RecommendationSummary> recommendations,
			List<ReviewSummary> reviews,
			ServiceAddresses serviceAddresses) {

		this.productId = productId;
		this.name = name;
		this.weight = weight;
		this.recommendations = recommendations;
		this.reviews = reviews;
		this.serviceAddresses = serviceAddresses;
	}

}
