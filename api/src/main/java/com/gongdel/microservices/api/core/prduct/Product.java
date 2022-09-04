package com.gongdel.microservices.api.core.prduct;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
public class Product {

	private int productId;
	private String name;
	private int weight;
	private String serviceAddress;
}
