package com.sanketdaru.poc.asyncjob.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

	public static boolean writeToFile(File file, String contents) {
		LOGGER.debug("Writing to file: {}", file.getAbsoluteFile());
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(contents);
		} catch (IOException e) {
			LOGGER.error("Error while writing to file.", e);
			return false;
		}
		return true;
	}

	public static String fetchFileContents(File file) throws IOException {
		LOGGER.debug("Reading from file: {}", file.getAbsoluteFile());
		StringBuilder fileContents = new StringBuilder();
		try (BufferedReader br = Files.newBufferedReader(Paths.get(file.getAbsolutePath()))) {
			String line;
			while ((line = br.readLine()) != null) {
				fileContents.append(line);
				fileContents.append(' ');
			}
		}
		
		return fileContents.toString();
	}
}
