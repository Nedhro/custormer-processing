# Customer Processing Project

The Customer Processing Project is designed to process a large-scale customer data file, filter and store the valid customers in a database, and export both valid and invalid customers to separate files. The project is implemented with efficient data processing techniques and multithreading for improved performance.

## Technology Used

- Java Development Kit (JDK) [17]
- Apache Maven [3.9.2]
- MongoDB (for database storage)

## Installation Process

1. Clone the project repository:

```shell
git clone https://github.com/Nedhro/custormer-processing
```
2. Navigate to the project directory:

```shell
cd customer-processing-project
```

3.Build the project using Maven:

```shell
mvn clean package or mvn clean install
```

4.Set up the MongoDB database:
Install MongoDB on your system (refer to the MongoDB documentation for installation instructions).
Start the MongoDB service.

## Configuration
Open the application.properties file located in the src/main/resources directory.
Configure the MongoDB connection settings:
- Set the spring.data.mongodb.host property to [MongoDB host].
- Set the spring.data.mongodb.port property to [MongoDB port].
- Set the spring.data.mongodb.database property to [customerdb]. (I have used)
- Set the spring.data.mongodb.username[root] and spring.data.mongodb.password[1234] properties if authentication is enabled.

## Usage
In the project resource folder the large-scale customer data file (1M-customers.txt) is designated.
Execute the following command to start the customer processing:
```shell
java -jar target/customer-processing-0.0.1-SNAPSHOT.jar
```
- The project will process the customer data,
- filter out duplicate and invalid customers based on their phone number and email, 
- store the valid customers in the database, and 
- export both valid and invalid customers to separate files.
- The exported files will be saved in the project directory.(output folder)

## Performance Metrics
The project provides performance metrics to measure the execution time for exporting files. The execution time is displayed in the console after the processing is completed.

## Contributing
Contributions to the Customer Processing Project are welcome! If you find any issues or have suggestions for improvements, please feel free to submit a pull request.

## License
This project is licensed under the MIT License.
