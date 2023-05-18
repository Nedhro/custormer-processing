package com.project.customerprocessing;

import com.project.customerprocessing.service.CustomerService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class CustomerProcessingApplication {
  private static final String OUTPUT_DIRECTORY = "output";
  private static boolean isFileProcessingCompleted = false; // Flag to track if processFile has been executed
  private static CustomerService customerService;

  public CustomerProcessingApplication(CustomerService customerService) {
    this.customerService = customerService;
  }

  public static void main(String[] args) {
    SpringApplication.run(CustomerProcessingApplication.class, args);
    File outputDir = new File(OUTPUT_DIRECTORY);
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    if (!isFileProcessingCompleted) {
      boolean success = customerService.processFile(OUTPUT_DIRECTORY);
      if (success) {
        isFileProcessingCompleted = true;
      } else {
        // Handle the case when processFile fails
        System.err.println("File processing failed.");
        // Log an error or perform necessary actions
      }
    } else {
      // Handle the case when processFile has already been executed
      System.out.println("File processing has already been completed.");
      // Log a message or perform necessary actions
    }
  }
}
