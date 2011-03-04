package com.c9a.catalog.web;

import com.google.protobuf.AbstractMessage.Builder;

public class RequestResponse {
	
	public RequestResponse(){};
	
	public RequestResponse(Builder<?> request, Builder<?> response){
		this.request = request;
		this.response = response;
	};
	private Builder<?> request;
	private Builder<?> response;
	public Builder<?> getRequest() {
		return request;
	}
	public void setRequest(Builder<?> request) {
		this.request = request;
	}
	public Builder<?> getResponse() {
		return response;
	}
	public void setResponse(Builder<?> response) {
		this.response = response;
	}
}