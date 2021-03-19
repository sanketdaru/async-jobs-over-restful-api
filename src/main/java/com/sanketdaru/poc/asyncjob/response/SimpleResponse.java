package com.sanketdaru.poc.asyncjob.response;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SimpleResponse extends BaseResponse {

	private static final long serialVersionUID = 6248820308968669361L;

	private String jobId;

	@JsonIgnore
	private File outputFile;

	private String outputFileURI;

	public SimpleResponse() {
		super();
	}

	public SimpleResponse(String jobId, RequestStatus requestStatus) {
		super(requestStatus);
		this.jobId = jobId;
	}

	public SimpleResponse(String jobId, RequestStatus requestStatus, File outputFile) {
		super(requestStatus);
		this.outputFile = outputFile;
		this.jobId = jobId;
	}

	public String getJobId() {
		return jobId;
	}

	public void setJobId(String jobId) {
		this.jobId = jobId;
	}

	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	public String getOutputFileURI() {
		return outputFileURI;
	}

	public void setOutputFileURI(String outputFileURI) {
		this.outputFileURI = outputFileURI;
	}

	@Override
	public String toString() {
		return "SimpleResponse [jobId=" + jobId + ", outputFile=" + outputFile + ", outputFileURI=" + outputFileURI
				+ "]";
	}

}
