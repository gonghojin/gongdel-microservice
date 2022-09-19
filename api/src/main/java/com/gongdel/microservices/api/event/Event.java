package com.gongdel.microservices.api.event;

import java.time.LocalDateTime;

import static java.time.LocalDateTime.now;

/**
 * @param <K>  데이터 식별을 위한 키(예: 제품 ID)
 * @param <T> 실제 이벤트 데이터
 */
public class Event<K, T> {

	public enum Type {CREATE, DELETE}

	private Event.Type eventType;
	private K key;
	private T data;
	private LocalDateTime eventCreatedAt;

	public Event() {
		this.eventType = null;
		this.key = null;
		this.data = null;
		this.eventCreatedAt = null;
	}

	public Event(Type eventType, K key, T data) {
		this.eventType = eventType;
		this.key = key;
		this.data = data;
		this.eventCreatedAt = now();
	}

	public Type getEventType() {
		return eventType;
	}

	public K getKey() {
		return key;
	}

	public T getData() {
		return data;
	}

	public LocalDateTime getEventCreatedAt() {
		return eventCreatedAt;
	}
}
