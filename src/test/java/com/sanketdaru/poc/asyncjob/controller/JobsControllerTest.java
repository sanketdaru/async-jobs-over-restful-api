package com.sanketdaru.poc.asyncjob.controller;

import com.sanketdaru.poc.asyncjob.service.JobsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JobsControllerTest {

    @Mock
    private JobsService jobsService;
    
    // AsyncJobsManager and MultipartFileHelper are also autowired in JobsController,
    // but not directly used by getJobOutputFile method. So, mocking them is optional here
    // unless a full context load is being attempted (which is not typical for unit tests like this).

    @InjectMocks
    private JobsController jobsController;

    @TempDir
    Path tempDir;

    @Test
    void testGetJobOutputFile_SuccessfulRetrieval() throws Throwable {
        String jobId = "test-job-output";
        String fileContent = "This is dummy content for the output file.";
        Path outputFilePath = tempDir.resolve(jobId + ".out");
        Files.writeString(outputFilePath, fileContent);
        File outputFile = outputFilePath.toFile();

        when(jobsService.getJobOutputFile(jobId)).thenReturn(outputFile);

        ResponseEntity<Resource> responseEntity = jobsController.getJobOutputFile(jobId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode(), "HTTP Status should be OK.");
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, responseEntity.getHeaders().getContentType(), "Content-Type should be OCTET_STREAM.");
        assertEquals(outputFile.length(), responseEntity.getHeaders().getContentLength(), "Content-Length should match file size.");
        
        assertNotNull(responseEntity.getBody(), "Response body should not be null.");
        assertTrue(responseEntity.getBody() instanceof InputStreamResource, "Body should be an InputStreamResource.");

        // To be absolutely sure, we can try to read the resource content, though this
        // also tests Spring's InputStreamResource and not strictly the controller's file closing.
        InputStreamResource resource = (InputStreamResource) responseEntity.getBody();
        String readContent = new String(resource.getInputStream().readAllBytes());
        assertEquals(fileContent, readContent, "Content read from resource should match original file content.");
        
        // The try-with-resources for FileInputStream is in the JobsController's implementation.
        // Its correct closure is implicitly tested by the fact that we can read the file
        // and no "file locked" or "stream closed" errors occur prematurely from the resource.
        // Direct verification of stream closure by try-with-resources is a language feature guarantee.
    }

    @Test
    void testGetJobOutputFile_JobsServiceThrowsFileNotFound() throws Throwable {
        String jobId = "test-job-notfound";
        FileNotFoundException expectedException = new FileNotFoundException("Mocked: Output file not found by service.");

        when(jobsService.getJobOutputFile(jobId)).thenThrow(expectedException);

        FileNotFoundException actualException = assertThrows(FileNotFoundException.class, () -> {
            jobsController.getJobOutputFile(jobId);
        }, "Expected JobsService's FileNotFoundException to be propagated.");
        
        assertSame(expectedException, actualException, "The propagated exception should be the same instance thrown by the service.");
    }
    
    @Test
    void testGetJobOutputFile_JobsServiceThrowsIOException() throws Throwable {
        String jobId = "test-job-ioexception";
        IOException expectedException = new IOException("Mocked: Generic IO error from service.");

        when(jobsService.getJobOutputFile(jobId)).thenThrow(expectedException);

        IOException actualException = assertThrows(IOException.class, () -> {
            jobsController.getJobOutputFile(jobId);
        }, "Expected JobsService's IOException to be propagated.");
        
        assertSame(expectedException, actualException, "The propagated exception should be the same instance thrown by the service.");
    }
}
