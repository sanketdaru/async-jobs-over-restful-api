package com.sanketdaru.poc.asyncjob.request;

import java.io.Serializable;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SimpleRequest implements Serializable {
	private static final long serialVersionUID = -1531579151241366498L;
	
	private String request;
	
	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleRequest [request=").append(request).append("]");
		return builder.toString();
	}
}
