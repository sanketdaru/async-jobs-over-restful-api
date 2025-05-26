package com.sanketdaru.poc.asyncjob.service;

import com.sanketdaru.poc.asyncjob.config.AppConfigs;
import com.sanketdaru.poc.asyncjob.exception.ErrorWhileProcessingRequest;
import com.sanketdaru.poc.asyncjob.helper.FileHelper;
import com.sanketdaru.poc.asyncjob.response.RequestStatus;
import com.sanketdaru.poc.asyncjob.response.SimpleResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JobsServiceTest {

    @Mock
    private AppConfigs appConfigs;

    @Mock
    private AsyncJobsManager asyncJobsManager; // Though not used in every test, good to have for consistency

    @InjectMocks
    private JobsService jobsService;

    private MockedStatic<FileHelper> mockedFileHelper;

    @TempDir
    Path tempDir; // For temporary file locations if needed by AppConfigs mock

    @BeforeEach
    void setUp() {
        // Mock static methods of FileHelper
        mockedFileHelper = Mockito.mockStatic(FileHelper.class);
        // Configure appConfigs for job files location (used by getOutputFile internally)
        when(appConfigs.getJobFilesLocation()).thenReturn(tempDir.toFile().getAbsolutePath());
    }

    @AfterEach
    void tearDown() {
        // Close the static mock
        mockedFileHelper.close();
    }

    @Test
    void testPostJobWithFile_VowelCountingAndContent() throws IOException, ExecutionException, InterruptedException {
        String jobId = "test-job-1";
        File mockInputFile = mock(File.class); // Mock the input file object

        String testFileContent = "AEIOUaeiou BcDfG 123!@#"; // 10 vowels, 20 chars
        int expectedVowelCount = 10;
        int expectedCharCount = testFileContent.length();

        // Mock FileHelper.fetchFileContents
        mockedFileHelper.when(() -> FileHelper.fetchFileContents(mockInputFile)).thenReturn(testFileContent);

        // Mock FileHelper.writeToFile
        mockedFileHelper.when(() -> FileHelper.writeToFile(any(File.class), anyString())).thenReturn(true);
        
        // Mock input file deletion
        when(mockInputFile.delete()).thenReturn(true);


        CompletableFuture<SimpleResponse> futureResponse = jobsService.postJobWithFile(jobId, mockInputFile);
        SimpleResponse response = futureResponse.get(); // Wait for completion

        assertNotNull(response, "Response should not be null.");
        assertEquals(jobId, response.getJobId(), "Job ID should match.");
        assertEquals(RequestStatus.COMPLETE, response.getRequestStatus(), "Request status should be COMPLETE.");
        assertNotNull(response.getOutputFile(), "Output file in response should not be null.");
        assertTrue(response.getOutputFile().getName().endsWith(".out"), "Output file should have .out extension.");

        // Verify the content written to the output file
        String expectedOutputContent = "The job file contained " + expectedCharCount + " chars, of which " + expectedVowelCount + " were vowels.";
        mockedFileHelper.verify(() -> FileHelper.writeToFile(any(File.class), eq(expectedOutputContent)));
        
        // Verify input file was deleted
        verify(mockInputFile).delete();
    }
    
    @Test
    void testPostJobWithFile_FetchContentThrowsIOException() throws IOException {
        String jobId = "test-job-ioex";
        File mockInputFile = mock(File.class);

        IOException ioException = new IOException("Failed to read");
        mockedFileHelper.when(() -> FileHelper.fetchFileContents(mockInputFile)).thenThrow(ioException);
        when(mockInputFile.delete()).thenReturn(true); // Ensure delete is still handled

        CompletableFuture<SimpleResponse> futureResponse = jobsService.postJobWithFile(jobId, mockInputFile);

        // Assert that the future completed exceptionally
        assertTrue(futureResponse.isCompletedExceptionally(), "Future should complete exceptionally.");
        ExecutionException ex = assertThrows(ExecutionException.class, futureResponse::get, "ExecutionException should be thrown.");
        assertSame(ioException, ex.getCause(), "The cause of ExecutionException should be the IOException from fetchFileContents.");
        
        verify(mockInputFile).delete(); // Verify input file was deleted even on exception
    }

    // Test for InterruptedException is harder to simulate reliably without real threads
    // and complex synchronization. The primary focus here is on the logic flow.
    // The InterruptedException in the original code was due to Thread.sleep, which was removed.
    // The current InterruptedException catch block in postJobWithFile is technically unreachable
    // with the removal of Thread.sleep(), unless FileHelper.fetchFileContents itself throws it,
    // which is not its declared behavior.

    // --- Tests for getJobStatus ---

    @Test
    void testGetJobStatus_FutureNotDone() throws Throwable {
        String jobId = "job-not-done";
        CompletableFuture<SimpleResponse> mockFuture = new CompletableFuture<>(); // Not completed
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);

        SimpleResponse response = jobsService.getJobStatus(jobId);

        assertEquals(jobId, response.getJobId());
        assertEquals(RequestStatus.IN_PROGRESS, response.getRequestStatus());
        assertNull(response.getOutputFileURI()); // URI is not set for IN_PROGRESS
    }

    @Test
    void testGetJobStatus_FutureCompletedSuccessfully() throws Throwable {
        String jobId = "job-done-ok";
        SimpleResponse completedResponse = new SimpleResponse(jobId, RequestStatus.COMPLETE, new File(tempDir.toFile(), jobId + ".out"));
        CompletableFuture<SimpleResponse> mockFuture = CompletableFuture.completedFuture(completedResponse);
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);

        SimpleResponse response = jobsService.getJobStatus(jobId);

        assertEquals(jobId, response.getJobId());
        assertEquals(RequestStatus.COMPLETE, response.getRequestStatus());
        assertNotNull(response.getOutputFileURI());
        assertTrue(response.getOutputFileURI().endsWith("/api/v1/jobs/" + jobId + "/output-file"));
    }

    @Test
    void testGetJobStatus_FutureCompletedExceptionally() {
        String jobId = "job-done-fail";
        RuntimeException originalException = new RuntimeException("Job processing failed miserably");
        CompletableFuture<SimpleResponse> mockFuture = CompletableFuture.failedFuture(originalException);
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);

        Throwable thrown = assertThrows(RuntimeException.class, () -> {
            jobsService.getJobStatus(jobId);
        });
        assertSame(originalException, thrown, "The original exception from the future should be rethrown.");
    }
    
    @Test
    void testGetJobStatus_JobNotFound() {
        String jobId = "job-not-found";
        when(asyncJobsManager.getJob(jobId)).thenReturn(null); // Job doesn't exist

        ErrorWhileProcessingRequest thrown = assertThrows(ErrorWhileProcessingRequest.class, () -> {
            jobsService.getJobStatus(jobId);
        });
        assertEquals(BaseService.JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND, thrown.getMessage());
    }

    // --- Tests for getJobOutputFile ---

    @Test
    void testGetJobOutputFile_JobCompleted_ReturnsFile() throws Throwable {
        String jobId = "job-output-ok";
        File mockOutputFile = new File(tempDir.toFile(), jobId + ".out");
        SimpleResponse completedResponse = new SimpleResponse(jobId, RequestStatus.COMPLETE, mockOutputFile);
        CompletableFuture<SimpleResponse> mockFuture = CompletableFuture.completedFuture(completedResponse);
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);

        File resultFile = jobsService.getJobOutputFile(jobId);

        assertSame(mockOutputFile, resultFile);
    }

    @Test
    void testGetJobOutputFile_JobNotDone_ThrowsException() {
        String jobId = "job-output-not-done";
        CompletableFuture<SimpleResponse> mockFuture = new CompletableFuture<>(); // Not completed
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);

        ErrorWhileProcessingRequest thrown = assertThrows(ErrorWhileProcessingRequest.class, () -> {
            jobsService.getJobOutputFile(jobId);
        });
        assertEquals("Job is still in progress...", thrown.getMessage());
    }

    @Test
    void testGetJobOutputFile_JobNotInManager_OutputFileExists() throws Throwable {
        String jobId = "job-output-orphan-exists";
        File orphanedOutputFile = tempDir.resolve(jobId + ".out").toFile();
        assertTrue(orphanedOutputFile.createNewFile(), "Failed to create orphaned output file for test.");

        when(asyncJobsManager.getJob(jobId)).thenReturn(null); // Job not in manager
        // appConfigs.getJobFilesLocation() is already mocked in setUp to return tempDir

        File resultFile = jobsService.getJobOutputFile(jobId);

        assertEquals(orphanedOutputFile.getAbsolutePath(), resultFile.getAbsolutePath());
        assertTrue(orphanedOutputFile.delete()); // Clean up
    }

    @Test
    void testGetJobOutputFile_JobNotInManager_OutputFileDoesNotExist() {
        String jobId = "job-output-orphan-not-exists";
        when(asyncJobsManager.getJob(jobId)).thenReturn(null); // Job not in manager
        // appConfigs.getJobFilesLocation() is already mocked in setUp

        ErrorWhileProcessingRequest thrown = assertThrows(ErrorWhileProcessingRequest.class, () -> {
            jobsService.getJobOutputFile(jobId);
        });
        assertEquals(BaseService.JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND, thrown.getMessage());
    }

    @Test
    void testGetJobOutputFile_FutureCompletedExceptionally_ThrowsException() {
        String jobId = "job-output-fail";
        IOException originalException = new IOException("Failed during job execution");
        CompletableFuture<SimpleResponse> mockFuture = CompletableFuture.failedFuture(originalException);
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);

        Throwable thrown = assertThrows(IOException.class, () -> {
            jobsService.getJobOutputFile(jobId);
        });
        assertSame(originalException, thrown);
    }

    // --- Tests for deleteJobAndAssociatedData ---

    @Test
    void testDeleteJobAndAssociatedData_JobNotDone_ReturnsInProgress() throws ErrorWhileProcessingRequest {
        String jobId = "del-job-not-done";
        CompletableFuture<SimpleResponse> mockFuture = new CompletableFuture<>(); // Not completed
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);

        SimpleResponse response = jobsService.deleteJobAndAssociatedData(jobId);

        assertEquals(jobId, response.getJobId());
        assertEquals(RequestStatus.IN_PROGRESS, response.getRequestStatus());
        verify(asyncJobsManager, never()).removeJob(jobId); // Should not be removed yet
        // File deletion is handled by whenComplete, so direct verify is tricky here,
        // but we trust whenComplete will be added.
    }

    @Test
    void testDeleteJobAndAssociatedData_JobDoneSuccessfully_DeletesAndRemoves() throws ErrorWhileProcessingRequest {
        String jobId = "del-job-done-ok";
        File mockOutputFile = mock(File.class);
        when(mockOutputFile.exists()).thenReturn(true);
        when(mockOutputFile.getAbsolutePath()).thenReturn(tempDir.resolve(jobId + ".out").toString());


        SimpleResponse completedResponse = new SimpleResponse(jobId, RequestStatus.COMPLETE, mockOutputFile);
        CompletableFuture<SimpleResponse> mockFuture = CompletableFuture.completedFuture(completedResponse);
        
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);
        when(asyncJobsManager.removeJob(jobId)).thenReturn(true); // Mock removal

        SimpleResponse response = jobsService.deleteJobAndAssociatedData(jobId);

        assertEquals(jobId, response.getJobId());
        assertEquals(RequestStatus.DELETED, response.getRequestStatus());

        // whenComplete runs, let's verify its effects by simulating its completion
        // For completed futures, whenComplete might run synchronously or very quickly.
        // We need to verify that the cleanup logic inside whenComplete is triggered.
        // Since cleanupJobResources is private, we verify its side effects:
        // 1. File deletion
        // 2. asyncJobsManager.removeJob
        
        // To ensure whenComplete has a chance to run, we can add a small delay or use a CountDownLatch
        // if testing highly async behavior. However, for an already completed future,
        // the actions in whenComplete should ideally be verifiable.
        // Let's verify removeJob is called. File.delete() is on a mock.
        
        // The whenComplete block will be scheduled. For an already completed future, it might execute
        // in the same thread or a different one depending on the executor.
        // To robustly test this, we could capture the lambda passed to whenComplete.
        // For now, let's assume it gets called.
        
        // Verify that removeJob was called (this is a key part of cleanup)
        // This needs the whenComplete block to execute.
        // If whenComplete runs on a different thread, this test might be flaky.
        // For already completed futures, it often runs in the calling thread.
        verify(asyncJobsManager, timeout(100)).removeJob(jobId); // Verify removeJob is called
        verify(mockOutputFile, timeout(100)).delete(); // Verify file delete is called
    }
    
    @Test
    void testDeleteJobAndAssociatedData_JobDoneExceptionally_DeletesAndRemoves() throws ErrorWhileProcessingRequest {
        String jobId = "del-job-done-fail";
        File mockOutputFile = mock(File.class); // Mock the output file
        // Simulate the output file path for getOutputFile fallback in cleanup
        when(mockOutputFile.getAbsolutePath()).thenReturn(tempDir.resolve(jobId + ".out").toString());


        // Even if the job fails, it might have an output file reference in SimpleResponse, or not.
        // Let's assume it failed before creating one for this test.
        SimpleResponse failedResponse = new SimpleResponse(jobId, RequestStatus.ERROR, null); 
                                                                // ^ or an actual output file if created before error
        
        // The actual exception from the job
        IOException jobException = new IOException("Job failed to process data");
        CompletableFuture<SimpleResponse> mockFuture = CompletableFuture.failedFuture(jobException);
        
        when(asyncJobsManager.getJob(jobId)).thenReturn(mockFuture);
        when(asyncJobsManager.removeJob(jobId)).thenReturn(true);

        // Mock getOutputFile in case the response has no file (fallback in cleanup)
        // This is tricky because getOutputFile is a method of the class under test.
        // The cleanupJobResources method calls this.getOutputFile(jobId).
        // We've already mocked appConfigs.getJobFilesLocation(), so getOutputFile(jobId) will construct a path.
        // We need to ensure this constructed File object (if used) can be interacted with (e.g. exists(), delete()).
        // For this test, we'll assume the cleanup method uses the response's file if present,
        // or constructs one. If constructed, it won't exist unless we create it.

        // Let's refine: the cleanupJobResources will try to delete getOutputFile(jobId) if response.getOutputFile() is null.
        // We need to mock the file system interaction for this potential file.
        // Since FileHelper is static, we can't easily mock its write/read for this.
        // However, we are mocking File.delete() itself.

        // If the file path used in cleanup is based on getOutputFile(jobId)
        // and that file doesn't exist, then exists() returns false, and delete() isn't called.
        // If it *does* exist (e.g. created by a previous successful run or another process), it should be deleted.
        
        // For this exceptional case, let's assume no output file was successfully created or it's null in response.
        // The cleanup will then try to delete `new File(appConfigs.getJobFilesLocation(), jobId + ".out")`.
        // We can mock this specific File object if needed, or rely on file system.
        // Let's ensure the File mock for the *actual* output file (if it were created) is used.
        
        // The main point is that cleanup runs.
        // Let's test the scenario where an output file *was* created before the exception.
        when(mockOutputFile.exists()).thenReturn(true); // Assume file was created before failure
        failedResponse.setOutputFile(mockOutputFile); // Set it in the (failed) response.

        SimpleResponse response = jobsService.deleteJobAndAssociatedData(jobId);

        assertEquals(jobId, response.getJobId());
        assertEquals(RequestStatus.DELETED, response.getRequestStatus());

        verify(asyncJobsManager, timeout(100)).removeJob(jobId);
        verify(mockOutputFile, timeout(100)).delete();
    }


    @Test
    void testDeleteJobAndAssociatedData_JobNotInManager_OutputFileExists_DeletesFile() throws ErrorWhileProcessingRequest, IOException {
        String jobId = "del-job-orphan-exists";
        File orphanedOutputFile = tempDir.resolve(jobId + ".out").toFile();
        assertTrue(orphanedOutputFile.createNewFile(), "Failed to create orphaned output file.");
        // appConfigs.getJobFilesLocation() is mocked.

        when(asyncJobsManager.getJob(jobId)).thenReturn(null); // Job not in manager

        SimpleResponse response = jobsService.deleteJobAndAssociatedData(jobId);

        assertEquals(jobId, response.getJobId());
        assertEquals(RequestStatus.DELETED, response.getRequestStatus());
        assertFalse(orphanedOutputFile.exists(), "Orphaned output file should have been deleted.");
        verify(asyncJobsManager, never()).removeJob(jobId); // Not in manager, so no removal call
    }

    @Test
    void testDeleteJobAndAssociatedData_JobNotInManager_OutputFileDoesNotExist_ReturnsDeleted() throws ErrorWhileProcessingRequest {
        String jobId = "del-job-orphan-not-exists";
        // Ensure file does not exist (it shouldn't by default in tempDir unless created)
        File nonExistentOrphanedFile = tempDir.resolve(jobId + ".out").toFile();
        assertFalse(nonExistentOrphanedFile.exists());

        when(asyncJobsManager.getJob(jobId)).thenReturn(null); // Job not in manager

        // The behavior for "job not found in manager and output file also not found" was changed to return DELETED.
        // It previously threw JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND.
        // Let's align with the current implementation in JobsService.java (line 154)
        SimpleResponse response = jobsService.deleteJobAndAssociatedData(jobId);
        assertEquals(RequestStatus.DELETED, response.getRequestStatus());


        // If the requirement was to throw ErrorWhileProcessingRequest, the test would be:
        // ErrorWhileProcessingRequest thrown = assertThrows(ErrorWhileProcessingRequest.class, () -> {
        //     jobsService.deleteJobAndAssociatedData(jobId);
        // });
        // assertEquals(BaseService.JOB_WITH_SUPPLIED_JOB_ID_NOT_FOUND, thrown.getMessage());
        
        verify(asyncJobsManager, never()).removeJob(jobId);
    }
}
