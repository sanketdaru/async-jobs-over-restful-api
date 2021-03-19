package com.sanketdaru.poc.asyncjob.service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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
				switch(fileContents.charAt(i)){
					case 'a':
					case 'A':
					case 'e':
					case 'E':
					case 'i':
					case 'I':
					case 'o':
					case 'O':
					case 'u':
					case 'U': numberOfVowels++;
				}
				
				Thread.sleep(100);
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

		Throwable[] errors = new Throwable[1];
		SimpleResponse[] simpleResponses = new SimpleResponse[1];
		completableFuture.whenComplete((response, ex) -> {
			if (ex != null) {
				errors[0] = ex.getCause();
			} else {
				StringBuilder outputFileUri = new StringBuilder(API_V1_JOBS);
				outputFileUri.append(jobId);
				outputFileUri.append(OUTPUT_FILE);
				response.setOutputFileURI(outputFileUri.toString());
				simpleResponses[0] = response;
			}
		});

		if (errors[0] != null) {
			throw errors[0];
		}

		return simpleResponses[0];
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

		Throwable[] errors = new Throwable[1];
		SimpleResponse[] simpleResponses = new SimpleResponse[1];
		completableFuture.whenComplete((response, ex) -> {
			if (ex != null) {
				errors[0] = ex.getCause();
			} else {
				simpleResponses[0] = response;
			}
		});

		if (errors[0] != null) {
			throw errors[0];
		}

		return simpleResponses[0].getOutputFile();
	}

	public SimpleResponse deleteJobAndAssociatedData(String jobId) throws ErrorWhileProcessingRequest {
		CompletableFuture<SimpleResponse> completableFuture = fetchJob(jobId);

		if (null == completableFuture) {
			File outputFile = getOutputFile(jobId);
			if(outputFile.exists()) {
				outputFile.delete();
				return new SimpleResponse(jobId, RequestStatus.DELETED);
			}
			
			throw new ErrorWhileProcessingRequest(JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND, true);
		}
		
		if (!completableFuture.isDone()) {
			return new SimpleResponse(jobId, RequestStatus.IN_PROGRESS);
		}

		completableFuture.whenComplete((response, ex) -> {
			if (ex != null) {
				LOGGER.error("Job failed with exception.", ex);
			}

			if (null != response && null != response.getOutputFile()) {
				if (response.getOutputFile().exists()) {
					response.getOutputFile().delete();
				}
			} else {
				File outputFile = getOutputFile(jobId);
				if(outputFile.exists()) {
					outputFile.delete();
				}
			}

			asyncJobsManager.removeJob(jobId);
		});

		return new SimpleResponse(jobId, RequestStatus.DELETED);
	}
	
	private File getOutputFile(String jobId) {
		return new File(appConfigs.getJobFilesLocation(), jobId + ".out");
	}
}
