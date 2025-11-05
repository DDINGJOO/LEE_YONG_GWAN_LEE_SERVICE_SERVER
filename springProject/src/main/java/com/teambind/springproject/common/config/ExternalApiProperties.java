package com.teambind.springproject.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 외부 API 설정 프로퍼티.
 */
@Component
@ConfigurationProperties(prefix = "external.api")
public class ExternalApiProperties {
	
	private PlaceInfoApi placeInfo = new PlaceInfoApi();
	
	public PlaceInfoApi getPlaceInfo() {
		return placeInfo;
	}
	
	public void setPlaceInfo(PlaceInfoApi placeInfo) {
		this.placeInfo = placeInfo;
	}
	
	/**
	 * Place Info Service API 설정.
	 */
	public static class PlaceInfoApi {
		private String url;
		private int connectTimeout = 5000;
		private int readTimeout = 5000;
		
		public String getUrl() {
			return url;
		}
		
		public void setUrl(String url) {
			this.url = url;
		}
		
		public int getConnectTimeout() {
			return connectTimeout;
		}
		
		public void setConnectTimeout(int connectTimeout) {
			this.connectTimeout = connectTimeout;
		}
		
		public int getReadTimeout() {
			return readTimeout;
		}
		
		public void setReadTimeout(int readTimeout) {
			this.readTimeout = readTimeout;
		}
	}
}
