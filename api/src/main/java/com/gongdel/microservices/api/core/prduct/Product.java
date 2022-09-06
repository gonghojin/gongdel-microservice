package com.gongdel.microservices.api.core.prduct;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
public class Product {

	private int productId;
	private String name;
	private int weight;
	private String serviceAddress;

	public int getProductId() {
		return productId;
	}

	public String getName() {
		return name;
	}

	public int getWeight() {
		return weight;
	}

	public String getServiceAddress() {
		return serviceAddress;
	}
}
