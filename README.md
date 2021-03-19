# async-jobs-over-restful-api
Submit long running jobs for asynchronous processing over a RESTful API.

# Setup

## Clone the repository
```
$ git clone git@github.com:sanketdaru/async-jobs-over-restful-api.git
```

## Change to directory with cloned repository
```
$ cd async-jobs-over-restful-api
```

## Ensure JDK 11 or later is installed
```
$ java -version
openjdk version "15.0.1" 2020-10-20
OpenJDK Runtime Environment Corretto-15.0.1.9.1 (build 15.0.1+9)
OpenJDK 64-Bit Server VM Corretto-15.0.1.9.1 (build 15.0.1+9, mixed mode, sharing)
```

# Run project

To change where the uploaded job files as well as resulting output files are stored on filesystem, you can modify the `app-configs.jobFilesLocation` property in the `application.properties` file.

Using gradle-wrapper run the SpringBoot application
```
$ ./gradlew bootRun
```
or if you're on Windows
```
$ gradlew.bat bootRun
```

This will run the embedded Tomcat bound to `localhost` and listening on port `8080`

*NOTE:* Gradle will show `<==========---> 80% EXECUTING` and just appears as if it hung! Don't worry. This is default behaviour of Gradle while running SpringBoot applications. A more meaninfgul log entry to notice is `Started AsyncJobsOverRestfulApi in 1.885 seconds (JVM running for 2.172)` after which you can proceed with tests. Gradle stays at 80% (or any random xx%) simply waiting for the application to be killed. To kill the application hit `ctrl+c`

# Test

Use your favourite REST client to issue requests and check the output. For the simplistic use-case that it is, `curl` can also be used.

1. POST a new job submit a file named sample.txt for asynchronous processing
``` curl
curl --location --request POST 'http://localhost:8080/api/v1/jobs' \
--form 'file=@"sample.txt"'
```
As soon as a new job is posted by client a response is received immediately. Notice the `nio-8080-exec-` thread name which belongs to Tomcat handling the incoming request...
```
[nio-8080-exec-1] c.s.p.a.controller.JobsController        : Received request for asynchronous file processing.
[nio-8080-exec-1] c.s.p.a.controller.JobsController        : Generated job-id 26e62d65-3e6a-4aab-8d79-a64f1eeee783 for this request.
[nio-8080-exec-1] c.s.p.a.controller.JobsController        : Job-id 26e62d65-3e6a-4aab-8d79-a64f1eeee783 submitted for processing. Returning from controller.
```
... however the actual job is executed asynchronously in another thread named `MyAsyncThread-`
```
[MyAsyncThread-1] c.s.poc.asyncjob.service.JobsService     : Received request with job-id 26e62d65-3e6a-4aab-8d79-a64f1eeee783 and file /var/tmp/26e62d65-3e6a-4aab-8d79-a64f1eeee783.in
[MyAsyncThread-1] c.s.poc.asyncjob.helper.FileHelper       : Reading from file: /var/tmp/26e62d65-3e6a-4aab-8d79-a64f1eeee783.in
[MyAsyncThread-1] c.s.poc.asyncjob.helper.FileHelper       : Writing to file: /var/tmp/26e62d65-3e6a-4aab-8d79-a64f1eeee783.out
[MyAsyncThread-1] c.s.poc.asyncjob.service.JobsService     : Completed processing the request.
```

2. GET the status of the posted asynchronous job. Use `job_id` received as response from posting a new job command as an input to next command
``` curl
curl --location --request GET 'http://localhost:8080/api/v1/jobs/{job_id}'
```
Once the job status is COMPLETE, you can check the location configured by `app-configs.jobFilesLocation` property to inspect the job output file.

3. GET the output file produced as a result of completion of the posted asynchronous job. Use the `output_file_uri` received as response from get the status command as an input to next command
``` curl
curl --location --request GET 'http://localhost:8080/{output_file_uri}'
```

4. DELETE the job as well as the output file. Use `job_id` received as response from posting a new job command as an input to next command
``` curl
curl --location --request DELETE 'http://localhost:8080/api/v1/jobs/{job_id}'
```
You can check the location configured by `app-configs.jobFilesLocation` property to ensure the job output file is really deleted.
