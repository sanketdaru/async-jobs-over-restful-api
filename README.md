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

## Ensure you have JDK 11 or later installed and set as JAVA_HOME.
```
$ java -version
openjdk version "15.0.1" 2020-10-20
OpenJDK Runtime Environment Corretto-15.0.1.9.1 (build 15.0.1+9)
OpenJDK 64-Bit Server VM Corretto-15.0.1.9.1 (build 15.0.1+9, mixed mode, sharing)
```

# Run project

Using gradle-wrapper run the SpringBoot project
```
$ ./gradlew bootRun
```
or if you're on Windows
```
$ gradlew.bat bootRun
```

This will run the embedded Tomcat bound to `localhost` and listening on port `8080`

# Test

Use your favourite REST client to issue requests and check the output. For the simplistic use-case that it is, `curl` can also be used.

``` curl
curl --location --request POST 'http://localhost:8080/api/v1/jobs' \
--form 'file=@"sample.txt"'
```

Use `job_id` received as response from above command as an input to next command
``` curl
curl --location --request GET 'http://localhost:8080/api/v1/jobs/{job_id}'
```

Use the `output_file_uri` received as response from above command as an input to next command
``` curl
curl --location --request GET 'http://localhost:8080/{output_file_uri}'
```
