package com.sanketdaru.poc.asyncjob.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app-configs")
public class AppConfigs {

	private String jobFilesLocation;

	public String getJobFilesLocation() {
		return jobFilesLocation;
	}

	public void setJobFilesLocation(String jobFilesLocation) {
		this.jobFilesLocation = jobFilesLocation;
	}
}
