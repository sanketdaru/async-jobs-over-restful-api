package com.sanketdaru.poc.asyncjob.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sanketdaru.poc.asyncjob.config.AppConfigs;
import com.sanketdaru.poc.asyncjob.exception.ErrorWhileProcessingRequest;
import com.sanketdaru.poc.asyncjob.helper.FileHelper;
import com.sanketdaru.poc.asyncjob.response.RequestStatus;
import com.sanketdaru.poc.asyncjob.response.SimpleResponse;

@Service
public class JobsService extends BaseService {
	private static final Logger LOGGER = LoggerFactory.getLogger(JobsService.class);

	private static final String API_V1_JOBS = "/api/v1/jobs/";
	private static final String OUTPUT_FILE = "/output-file";
	
	@Autowired
	AppConfigs appConfigs;
	
	@Async("asyncTaskExecutor")
	public CompletableFuture<SimpleResponse> postJobWithFile(String jobId, File file) {
		LOGGER.info("Received request with job-id {} and file {}", jobId, file);

		CompletableFuture<SimpleResponse> task =  new CompletableFuture<SimpleResponse>();
		
		try {
			int numberOfVowels = 0;
			String fileContents = FileHelper.fetchFileContents(file);
			
			// Trivial loop to demonstrate a long-running task
			for (int i=0; i<fileContents.length(); i++) {
				switch(Character.toLowerCase(fileContents.charAt(i))){
					case 'a':
					case 'e':
					case 'i':
					case 'o':
					case 'u': numberOfVowels++;
				}
			}
			
			StringBuilder outputFileContents =  new StringBuilder();
			outputFileContents.append("The job file contained ");
			outputFileContents.append(fileContents.length());
			outputFileContents.append(" chars, of which ");
			outputFileContents.append(numberOfVowels);
			outputFileContents.append(" were vowels.");
			
			File outputFile = getOutputFile(jobId);
			FileHelper.writeToFile(outputFile, outputFileContents.toString());
			task.complete(new SimpleResponse(jobId, RequestStatus.COMPLETE, outputFile));
		} catch (IOException e) {
			LOGGER.error("Error during file operation.", e);
			task.completeExceptionally(e);
		} catch (InterruptedException e) {
			LOGGER.error("Error while counting characters in file.", e);
			task.completeExceptionally(e);
		} finally {
			file.delete();
		}
		
		LOGGER.info("Completed processing the request.");
		return task;
	}
	
	public SimpleResponse getJobStatus(String jobId) throws Throwable {
		CompletableFuture<SimpleResponse> completableFuture = fetchJobElseThrowException(jobId);

		if (!completableFuture.isDone()) {
			return new SimpleResponse(jobId, RequestStatus.IN_PROGRESS);
		}

		// If future is done, get the result directly
		try {
			SimpleResponse response = completableFuture.get();
			StringBuilder outputFileUri = new StringBuilder(API_V1_JOBS);
			outputFileUri.append(jobId);
			outputFileUri.append(OUTPUT_FILE);
			response.setOutputFileURI(outputFileUri.toString());
			return response;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Preserve interrupt status
			LOGGER.error("Job status retrieval interrupted.", e);
			throw new ErrorWhileProcessingRequest("Job status retrieval interrupted for job ID: " + jobId, e, true);
		} catch (ExecutionException e) {
			LOGGER.error("Job execution failed for job ID: " + jobId, e.getCause());
			throw e.getCause();
		}
	}
	
	public File getJobOutputFile(String jobId) throws Throwable {
		CompletableFuture<SimpleResponse> completableFuture = fetchJob(jobId);

		if (null == completableFuture) {
			File outputFile = getOutputFile(jobId);
			if(outputFile.exists()) {
				return outputFile;
			}
			
			throw new ErrorWhileProcessingRequest(JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND, true);
		}

		if (!completableFuture.isDone()) {
			throw new ErrorWhileProcessingRequest("Job is still in progress...", true);
		}

		// If future is done, get the result directly
		try {
			SimpleResponse response = completableFuture.get();
			return response.getOutputFile();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Preserve interrupt status
			LOGGER.error("Job output file retrieval interrupted.", e);
			throw new ErrorWhileProcessingRequest("Job output file retrieval interrupted for job ID: " + jobId, e, true);
		} catch (ExecutionException e) {
			LOGGER.error("Job execution failed for job ID: " + jobId, e.getCause());
			throw e.getCause();
		}
	}

	public SimpleResponse deleteJobAndAssociatedData(String jobId) throws ErrorWhileProcessingRequest {
		CompletableFuture<SimpleResponse> completableFuture = fetchJob(jobId);

		if (null == completableFuture) {
			File outputFile = getOutputFile(jobId);
			if (outputFile.exists()) {
				if (outputFile.delete()) {
					LOGGER.info("Deleted output file for non-tracked job ID: {}", jobId);
				} else {
					LOGGER.warn("Could not delete output file for non-tracked job ID: {}", jobId);
				}
				return new SimpleResponse(jobId, RequestStatus.DELETED);
			}
			throw new ErrorWhileProcessingRequest(JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND, true);
		}
		
		// Attach cleanup logic to the CompletableFuture.
		// This will run when the future completes, whether it's already done or completes later.
		completableFuture.whenComplete((response, ex) -> {
			cleanupJobResources(jobId, response, ex);
		});

		// If the job is not yet done, report IN_PROGRESS.
		// The cleanup will be handled by whenComplete when the job eventually finishes.
		if (!completableFuture.isDone()) {
			return new SimpleResponse(jobId, RequestStatus.IN_PROGRESS);
		}
		
		// If the job is already done, return DELETED status.
		// The whenComplete attached above will handle the cleanup.
		return new SimpleResponse(jobId, RequestStatus.DELETED);
	}

	/**
	 * Helper method to clean up job resources (output file and remove from manager).
	 * This is called when a job completes (successfully or exceptionally).
	 */
	private void cleanupJobResources(String jobId, SimpleResponse response, Throwable ex) {
		if (ex != null) {
			// If ex is ExecutionException, its cause is the actual exception from the async task
			Throwable cause = ex.getCause();
			LOGGER.error("Job ID: {} processing finished with exception. Cleaning up associated data.", jobId, cause != null ? cause : ex);
		} else {
			LOGGER.info("Job ID: {} processing finished successfully. Cleaning up associated data.", jobId);
		}

		File outputFileToDelete = null;
		// Attempt to get output file from response, otherwise construct path
		if (response != null && response.getOutputFile() != null) {
			outputFileToDelete = response.getOutputFile();
		} else {
			outputFileToDelete = getOutputFile(jobId); // Fallback for jobs that failed early or response is minimal
		}

		if (outputFileToDelete != null && outputFileToDelete.exists()) {
			if (outputFileToDelete.delete()) {
				LOGGER.info("Successfully deleted output file: {} for job ID: {}", outputFileToDelete.getAbsolutePath(), jobId);
			} else {
				LOGGER.warn("Could not delete output file: {} for job ID: {}", outputFileToDelete.getAbsolutePath(), jobId);
			}
		} else {
			LOGGER.info("No output file to delete or file already deleted for job ID: {}", jobId);
		}
		
		// Always attempt to remove the job from the manager after its completion and cleanup attempt
		boolean removed = asyncJobsManager.removeJob(jobId);
		if(removed) {
			LOGGER.info("Removed job ID: {} from async jobs manager.", jobId);
		} else {
			// This can be normal if the job was already removed or completed very quickly
			LOGGER.info("Job ID: {} was not found in async jobs manager for removal (may have been removed prior or completed rapidly).", jobId);
		}
	}
	
	private File getOutputFile(String jobId) {
		return new File(appConfigs.getJobFilesLocation(), jobId + ".out");
	}
}
		return new File(appConfigs.getJobFilesLocation(), jobId + ".out");
	}
}
