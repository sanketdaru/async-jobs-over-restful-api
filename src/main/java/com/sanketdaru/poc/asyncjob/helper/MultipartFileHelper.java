package com.sanketdaru.poc.asyncjob.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sanketdaru.poc.asyncjob.config.AppConfigs;

@Service
public class MultipartFileHelper {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MultipartFileHelper.class);
	
	private static final String EXTENSION = ".in";
	
	@Autowired
	AppConfigs appConfigs;
	
	public File saveUploadedFile(MultipartFile file, String jobId) {
		try {
			Path filePath = Paths.get(appConfigs.getJobFilesLocation(), jobId + EXTENSION);
			Files.copy(file.getInputStream(), filePath);
			return filePath.toFile();
		} catch (IOException e) {
			LOGGER.error("Error while copying the contents of uploaded file to disk.", e);
		}
		return null;
	}
}