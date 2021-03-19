package com.sanketdaru.poc.asyncjob.service;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sanketdaru.poc.asyncjob.exception.ErrorWhileProcessingRequest;
import com.sanketdaru.poc.asyncjob.response.SimpleResponse;

@Service
public class BaseService {
	private static final Logger LOGGER = LoggerFactory.getLogger(JobsService.class);
	
	protected static final String JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND = "Job with supplied job-id not found!";

	@Autowired
	protected AsyncJobsManager asyncJobsManager;

	/**
	 * 
	 * @param jobId
	 * @return the CompletableFuture associated with the jobId. This method will
	 *         throw an exception if the job does not exist.
	 * @throws ErrorWhileProcessingRequest
	 */
	public CompletableFuture<SimpleResponse> fetchJobElseThrowException(String jobId) throws ErrorWhileProcessingRequest {
		CompletableFuture<SimpleResponse> job = fetchJob(jobId);
		if(null == job) {
			LOGGER.error("Job-id {} not found.", jobId);
			throw new ErrorWhileProcessingRequest(JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND, true);
		}
		return job;
	}

	/**
	 * 
	 * @param jobId
	 * @param throwErrorIfNotFound
	 * @return the CompletableFuture associated with the jobId. This method will
	 *         throw an exception if the job does not exist and throwErrorIfNotFound
	 *         is supplied as true.
	 * @throws ErrorWhileProcessingRequest
	 */
	public CompletableFuture<SimpleResponse> fetchJob(String jobId) {
		@SuppressWarnings("unchecked")
		CompletableFuture<SimpleResponse> completableFuture = (CompletableFuture<SimpleResponse>) asyncJobsManager
				.getJob(jobId);

		return completableFuture;
	}
}
