package com.sanketdaru.poc.asyncjob.exception;

public class ErrorWhileProcessingRequest extends Exception {

	private static final long serialVersionUID = -7708713788971604015L;
	
	private boolean badRequest = false;

	public ErrorWhileProcessingRequest() {
		super();
	}

	public ErrorWhileProcessingRequest(String message) {
		super(message);
	}

	public ErrorWhileProcessingRequest(String message, boolean badRequest) {
		super(message);
		this.badRequest = badRequest;
	}

	public boolean isBadRequest() {
		return badRequest;
	}
}
