package com.sanketdaru.poc.asyncjob.response;

public enum RequestStatus {

	SUBMITTED(1), IN_PROGRESS(2), COMPLETE(3), ERROR(4), DELETED(5), UNKNOWN(0);

	private Integer value;

	RequestStatus(Integer value) {
		this.value = value;
	}

	public Integer value() {
		return value;
	}

	public static RequestStatus fromValue(int value) {
		switch (value) {
		case 1:
			return SUBMITTED;
		case 2:
			return IN_PROGRESS;
		case 3:
			return COMPLETE;
		case 4:
			return ERROR;
		case 5:
			return DELETED;
		}
		return UNKNOWN;
	}
}
