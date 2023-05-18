package com.project.customerprocessing.service;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.project.customerprocessing.model.Customer;
import com.project.customerprocessing.model.dto.CustomerDto;
import com.project.customerprocessing.repository.CustomerRepository;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service class for processing customers.
 */
@Service
public class CustomerService {
  
  private final MongoTemplate mongoTemplate;
  private final ResourceLoader resourceLoader;
  private final CustomerRepository customerRepository;
  private final ExecutorService executor = Executors.newFixedThreadPool(4);
  
  /**
   * Constructor for CustomerService.
   *
   * @param mongoTemplate  the MongoTemplate instance
   * @param resourceLoader the ResourceLoader instance
   */
  public CustomerService(MongoTemplate mongoTemplate, ResourceLoader resourceLoader, CustomerRepository customerRepository) {
    this.mongoTemplate = mongoTemplate;
    this.resourceLoader = resourceLoader;
    this.customerRepository = customerRepository;
  }
  
  /**
   * Created for future use
   */
  public Customer saveCustomer(Customer customer) {
    return customerRepository.save(customer);
  }
  
  public Optional<Customer> getCustomerById(String customerId) {
    return customerRepository.findById(customerId);
  }
  
  public List<Customer> getAllCustomers() {
    return customerRepository.findAll();
  }
  
  public void deleteCustomerById(String customerId) {
    customerRepository.deleteById(customerId);
  }
  /**/
  
  /**
   * Process the customer data from a file.
   *
   * @param outputDirectory the output directory for exporting data
   */
  public boolean processFile(String outputDirectory) {
    try {
      List<CustomerDto> customers = readCustomersFromFile();
      assert customers != null;
      List<CustomerDto> validCustomers = filterValidCustomers(customers);
      List<CustomerDto> invalidCustomers = filterInvalidCustomers(customers);
      executor.submit(() -> storeValidCustomers(validCustomers));
      executor.submit(() -> storeInvalidCustomers(invalidCustomers));
      executor.submit(() -> exportValidCustomers(validCustomers, outputDirectory));
      executor.submit(() -> exportInvalidCustomers(invalidCustomers, outputDirectory));
      
      // Shut down the executor once all tasks are completed
      executor.shutdown();
      
      // Return true at the end if file processing is successful
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      // Return false if file processing fails
      return false;
    }
  }
  
  
  /**
   * Reads customer data from a file.
   *
   * @return the list of customers read from the file
   */
  private List<CustomerDto> readCustomersFromFile() {
    try {
      List<CustomerDto> customers = new ArrayList<>();
      List<CustomerDto> invalidCustomers = new ArrayList<>();
      
      Resource resource = resourceLoader.getResource("classpath:static/files/1M-customers.txt");
      
      try (InputStream inputStream = resource.getInputStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
        String line;
        while ((line = reader.readLine()) != null) {
          try {
            String[] customerData = line.split(",");
            CustomerDto customer = new CustomerDto();
            customer.setName(customerData[0]);
            customer.setBranch(customerData[1]);
            customer.setCity(customerData[2]);
            customer.setState(customerData[3]);
            customer.setZip(customerData[4]);
            customer.setPhone(customerData[5]);
            customer.setEmail(customerData[6]);
            customer.setIp(customerData[7]);
            customers.add(customer);
          } catch (ArrayIndexOutOfBoundsException e) {
            CustomerDto invalidCustomer = new CustomerDto();
            invalidCustomer.setName(line);
            invalidCustomers.add(invalidCustomer);
          }
        }
      }
      
      executor.submit(() -> storeInvalidCustomers(invalidCustomers));
      
      return customers;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }
  
  /**
   * Filters the valid customers from the given list.
   *
   * @param customers the list of customers
   * @return the list of valid customers
   */
  private List<CustomerDto> filterValidCustomers(List<CustomerDto> customers) {
    List<CustomerDto> validCustomers = new ArrayList<>();
    Set<String> phoneSet = new HashSet<>();
    Set<String> emailSet = new HashSet<>();
    
    for (CustomerDto customer : customers) {
      if (isValidCustomer(customer, phoneSet, emailSet)) {
        validCustomers.add(customer);
        phoneSet.add(customer.getPhone());
        emailSet.add(customer.getEmail());
      }
    }
    
    return validCustomers;
  }
  
  /**
   * Checks if a customer is valid based on the provided criteria.
   *
   * @param customer the customer to validate
   * @param phoneSet the set of unique phone numbers
   * @param emailSet the set of unique email addresses
   * @return true if the customer is valid, false otherwise
   */
  private boolean isValidCustomer(CustomerDto customer, Set<String> phoneSet, Set<String> emailSet) {
    return isValidPhoneNumber(customer.getPhone()) && isValidEmail(customer.getEmail()) && !phoneSet.contains(customer.getPhone()) && !emailSet.contains(customer.getEmail());
  }
  
  /**
   * Filters the invalid customers from the given list.
   *
   * @param customers the list of customers
   * @return the list of invalid customers
   */
  private List<CustomerDto> filterInvalidCustomers(List<CustomerDto> customers) {
    List<CustomerDto> invalidCustomers = new ArrayList<>();
    Set<String> phoneSet = new HashSet<>();
    Set<String> emailSet = new HashSet<>();
    
    for (CustomerDto customer : customers) {
      if (!isValidCustomer(customer, phoneSet, emailSet)) {
        invalidCustomers.add(customer);
      }
    }
    return invalidCustomers;
  }
  
  /**
   * Stores the valid customers to the database.
   *
   * @param validCustomers the list of valid customers
   */
  private void storeValidCustomers(List<CustomerDto> validCustomers) {
    MongoCollection<Document> validCustomersCollection = mongoTemplate.getCollection("valid_customers");
    storeToDB(validCustomers, validCustomersCollection);
  }
  
  /**
   * Stores the invalid customers to the database.
   *
   * @param invalidCustomers the list of invalid customers
   */
  private void storeInvalidCustomers(List<CustomerDto> invalidCustomers) {
    MongoCollection<Document> invalidCustomersCollection = mongoTemplate.getCollection("invalid_customers");
    storeToDB(invalidCustomers, invalidCustomersCollection);
  }
  
  /**
   * Stores a list of customers to the specified database collection.
   *
   * @param customerDtoList     the list of customers to store
   * @param customersCollection the database collection to store the customers
   */
  private void storeToDB(List<CustomerDto> customerDtoList, MongoCollection<Document> customersCollection) {
    for (CustomerDto customerDto : customerDtoList) {
      Document filter = new Document("email", customerDto.getEmail()).append("phone", customerDto.getPhone());
      Document update = new Document("$set", new Document("name", customerDto.getName())
          .append("branch", customerDto.getBranch())
          .append("city", customerDto.getCity())
          .append("state", customerDto.getState())
          .append("zip", customerDto.getZip())
          .append("ip", customerDto.getIp()));
      customersCollection.findOneAndUpdate(filter, update, new FindOneAndUpdateOptions().upsert(true));
    }
  }
  
  /**
   * Exports the valid customers to files in batches.
   *
   * @param validCustomers  the list of valid customers
   * @param outputDirectory the output directory for exporting data
   */
  private void exportValidCustomers(List<CustomerDto> validCustomers, String outputDirectory) {
    long startTime = System.currentTimeMillis();
    int batchSize = 100000;
    int batchCount = (int) Math.ceil((double) validCustomers.size() / batchSize);
    
    for (int i = 0; i < batchCount; i++) {
      int startIndex = i * batchSize;
      int endIndex = Math.min(startIndex + batchSize, validCustomers.size());
      List<CustomerDto> batchCustomers = validCustomers.subList(startIndex, endIndex);
      
      String fileName = outputDirectory + "/valid_customers_batch_" + (i + 1) + ".txt";
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
        for (CustomerDto customer : batchCustomers) {
          String line = customer.getName() + "," + customer.getBranch() + "," + customer.getCity() + "," + customer.getState() + "," + customer.getZip() + "," + customer.getPhone() + "," + customer.getEmail() + "," + customer.getIp();
          writer.write(line);
          writer.newLine();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    long endTime = System.currentTimeMillis();
    System.out.println("Exporting valid customers took " + (endTime - startTime) + "ms.");
  }
  
  /**
   * Exports the invalid customers to files in batches.
   *
   * @param invalidCustomers the list of invalid customers
   * @param outputDirectory  the output directory for exporting data
   */
  private void exportInvalidCustomers(List<CustomerDto> invalidCustomers, String outputDirectory) {
    long startTime = System.currentTimeMillis();
    int batchSize = 100000;
    int batchCount = (int) Math.ceil((double) invalidCustomers.size() / batchSize);
    
    for (int i = 0; i < batchCount; i++) {
      int startIndex = i * batchSize;
      int endIndex = Math.min(startIndex + batchSize, invalidCustomers.size());
      List<CustomerDto> batchCustomers = invalidCustomers.subList(startIndex, endIndex);
      
      String fileName = outputDirectory + "/invalid_customers_batch_" + (i + 1) + ".txt";
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
        for (CustomerDto customer : batchCustomers) {
          writer.write(customer.getName());
          writer.newLine();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    
    long endTime = System.currentTimeMillis();
    System.out.println("Exporting invalid customers took " + (endTime - startTime) + "ms.");
  }
  
  /**
   * Checks if a phone number is valid.
   *
   * @param phone the phone number to validate
   * @return true if the phone number is valid, false otherwise
   */
  private boolean isValidPhoneNumber(String phone) {
    // Remove any non-digit characters from the phone number
    String numericPhone = phone.replaceAll("\\D", "");
    return StringUtils.isNumeric(numericPhone) && numericPhone.length() == 10;
  }
  
  /**
   * Checks if an email address is valid.
   *
   * @param email the email address to validate
   * @return true if the email address is valid, false otherwise
   */
  private boolean isValidEmail(String email) {
    // Regex pattern for email validation
    String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    
    // Validate email against the pattern
    return StringUtils.isNotBlank(email) && email.matches(emailRegex);
  }
}
