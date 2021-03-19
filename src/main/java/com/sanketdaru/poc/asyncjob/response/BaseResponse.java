package com.sanketdaru.poc.asyncjob.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.io.Serializable;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BaseResponse implements Serializable {

	private static final long serialVersionUID = 1723692267713314756L;

	private RequestStatus requestStatus;

	public BaseResponse() {
	}

	public BaseResponse(RequestStatus requestStatus) {
		super();
		this.requestStatus = requestStatus;
	}

	public RequestStatus getRequestStatus() {
		return requestStatus;
	}

	public void setRequestStatus(RequestStatus requestStatus) {
		this.requestStatus = requestStatus;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BaseResponse [requestStatus=").append(requestStatus).append("]");
		return builder.toString();
	}
}
