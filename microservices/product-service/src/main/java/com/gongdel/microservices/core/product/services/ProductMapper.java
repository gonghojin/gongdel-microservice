package com.gongdel.microservices.core.product.services;

import com.gongdel.microservices.api.core.prduct.Product;
import com.gongdel.microservices.core.product.persistence.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "string")
public interface ProductMapper {

	@Mappings({
			@Mapping(target = "serviceAddress", ignore = true)
	})
	Product entityToApi(ProductEntity entity);

	@Mappings({
			@Mapping(target = "id", ignore = true),
			@Mapping(target = "version", ignore = true)
	})
	ProductEntity apiToEntity(Product api);
}
