package com.gongdel.microservices.composite.product.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gongdel.microservices.api.event.Event;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

// Matcher를 직접 구현하여 사용해야 하는 경우 사용
public class IsSameEvent extends TypeSafeMatcher<String> {

	private Event expectedEvent;

	private ObjectMapper mapper = new ObjectMapper();

	private static final Logger LOG = LoggerFactory.getLogger(IsSameEvent.class);

	private IsSameEvent(Event expectedEvent) {
		this.expectedEvent = expectedEvent;
	}

	/*
	 * Matcher가 실제로 검증하는 로직 부분
	 */
	@Override
	protected boolean matchesSafely(String eventAsJson) {
		if (expectedEvent == null) {
			return false;
		}
		LOG.trace("Convert the following json string to a map: {}", eventAsJson);
		Map mapEvent = convertJsonStringToMap(eventAsJson);
		mapEvent.remove("eventCreatedAt");

		Map mapExpectedEvent = getMapWithoutCreatedAt(expectedEvent);
		LOG.trace("Got the map: {}", mapEvent);
		LOG.trace("Compare to the expected map: {}", mapExpectedEvent);

		return mapEvent.equals(mapExpectedEvent);
	}

	/*
	 * Matcher에 적합하지 않은 경우 나타나는 경고문
	 */
	@Override
	public void describeTo(Description description) {
		String expectedJson = convertObjectToJsonString(expectedEvent);
		description.appendText("expected to look like " + expectedJson);
	}

	public static Matcher<String> sameEventExceptCreatedAt(Event expectedEvent) {
		return new IsSameEvent(expectedEvent);
	}

	private Map convertJsonStringToMap(String eventAsJson) {
		try {
			return mapper.readValue(eventAsJson, new TypeReference<HashMap>() {
			});
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	private Map getMapWithoutCreatedAt(Event event) {
		Map mapEvent = convertObjectToMap(event);
		mapEvent.remove("eventCreatedAt");
		return mapEvent;
	}

	private Map convertObjectToMap(Object object) {
		JsonNode node = mapper.convertValue(object, JsonNode.class);
		return mapper.convertValue(node, Map.class);
	}

	private String convertObjectToJsonString(Object object) {
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}
}
