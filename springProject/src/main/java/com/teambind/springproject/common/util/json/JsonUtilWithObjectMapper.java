package com.teambind.springproject.common.util.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component("jsonUtilWithObjectMapper")
public class JsonUtilWithObjectMapper implements JsonUtil {
	private final ObjectMapper objectMapper;
	
	public JsonUtilWithObjectMapper() {
		this.objectMapper = new ObjectMapper();
		// Java 8 날짜/시간 타입 지원 활성화
		this.objectMapper.registerModule(new JavaTimeModule());
		// 날짜를 타임스탬프가 아닌 ISO-8601 문자열로 직렬화
		this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}
	
	public String toJson(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public <T> T fromJson(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
