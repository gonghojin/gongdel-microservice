package com.gongdel.microservices.composite.product.services;

import com.gongdel.microservices.api.event.Event;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class IsSameEvent extends TypeSafeMatcher<String> {

	private Event expectedEvent;

	private IsSameEvent(Event expectedEvent) {
		this.expectedEvent = expectedEvent;
	}

	@Override
	protected boolean matchesSafely(String item) {
		return false;
	}

	@Override
	public void describeTo(Description description) {

	}

	public static Matcher<String> sameEventExceptCreatedAt(Event expectedEvent) {
		return new IsSameEvent(expectedEvent);
	}
}
