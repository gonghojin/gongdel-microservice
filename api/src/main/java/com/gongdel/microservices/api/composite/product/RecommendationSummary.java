package com.gongdel.microservices.api.composite.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class RecommendationSummary {

	private final int recommendationId;
	private final String author;
	private final int rate;
	private final String content;

	public RecommendationSummary() {
		this.recommendationId = 0;
		this.author = null;
		this.rate = 0;
		this.content = null;
	}

	public RecommendationSummary(int recommendationId, String author, int rate, String content) {
		this.recommendationId = recommendationId;
		this.author = author;
		this.rate = rate;
		this.content = content;
	}
}
