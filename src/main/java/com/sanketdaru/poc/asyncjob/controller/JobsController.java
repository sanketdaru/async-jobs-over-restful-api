package com.sanketdaru.poc.asyncjob.controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sanketdaru.poc.asyncjob.exception.ErrorWhileProcessingRequest;
import com.sanketdaru.poc.asyncjob.helper.MultipartFileHelper;
import com.sanketdaru.poc.asyncjob.response.RequestStatus;
import com.sanketdaru.poc.asyncjob.response.SimpleResponse;
import com.sanketdaru.poc.asyncjob.service.AsyncJobsManager;
import com.sanketdaru.poc.asyncjob.service.JobsService;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(JobsController.class);
	
	@Autowired
	protected AsyncJobsManager asyncJobsManager;

	@Autowired
	private JobsService jobsService;

	@Autowired
	private MultipartFileHelper multipartFileHelper;

	@PostMapping(consumes = "multipart/form-data", produces = "application/json")
	public SimpleResponse postJobWithFile(@RequestParam("file") MultipartFile file) 
			throws Throwable {
		LOGGER.info("Received request for asynchronous file processing.");

		String jobId = UUID.randomUUID().toString();
		LOGGER.info("Generated job-id {} for this request.", jobId);
		
		if (null != jobsService.fetchJob(jobId)) {
			throw new ErrorWhileProcessingRequest("A job with same job-id already exists!", true);
		}

		File uploadedFile = multipartFileHelper.saveUploadedFile(file, jobId);
		if (null == uploadedFile) {
			throw new ErrorWhileProcessingRequest("Error occurred while reading the uploaded file.");
		}

		CompletableFuture<SimpleResponse> completableFuture = jobsService.postJobWithFile(jobId, uploadedFile);

		asyncJobsManager.putJob(jobId, completableFuture);

		LOGGER.info("Job-id {} submitted for processing. Returning from controller.", jobId);
		return new SimpleResponse(jobId, RequestStatus.SUBMITTED);
	}

	@GetMapping(path = "/{job-id}", produces = "application/json")
	public SimpleResponse getJobStatus(@PathVariable(name = "job-id") String jobId) throws Throwable {
		LOGGER.debug("Received request to fetch status of job-id: {}", jobId);

		return jobsService.getJobStatus(jobId);
	}

	@GetMapping(path = "/{job-id}/output-file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<Resource> getJobOutputFile(@PathVariable(name = "job-id") String jobId) throws Throwable {
		LOGGER.debug("Received request to fetch output file of job-id: {}", jobId);

		File outputFile = jobsService.getJobOutputFile(jobId);
		InputStreamResource resource = new InputStreamResource(new FileInputStream(outputFile));

		return ResponseEntity.ok()
				.contentLength(outputFile.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(resource);
	}

	@DeleteMapping(path = "/{job-id}", produces = "application/json")
	public SimpleResponse deleteJobAndAssociatedData(@PathVariable(name = "job-id") String jobId) throws Throwable {
		LOGGER.debug("Received request to delete job-id: {}", jobId);

		return jobsService.deleteJobAndAssociatedData(jobId);
	}
}
