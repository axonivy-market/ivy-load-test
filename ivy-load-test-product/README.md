# JMeter Java DSL Performance Test

![JMeter Java DSL Performance Test](doc.png)

This project provides demo performance tests as a reference for your Axon Ivy project using **JMeter Java DSL**, a modern Java-based approach to performance testing. It offers a fluent API for creating JMeter test plans programmatically, eliminating the need for XML-based test plans by allowing you to write tests in pure Java code.

**Key Benefits:**
- **Type Safety**: Compile-time validation of test plans
- **IDE Support**: Full debugging capabilities
- **Maintainability**: Easy refactoring and code reuse
- **Version Control Friendly**: Pure Java code instead of XML files
- **Integration**: Seamless integration with JUnit and testing frameworks

## Demo

### DemoTest

A simple smoke test that verifies external website availability:
- Sends HTTP GET requests to `axonivy.com` and `market.axonivy.com`
- Validates HTTP 200 response codes and expected page content
- Generates JTL result files and HTML reports

### PerformancePortalTest

A full Portal walkthrough test simulating a real user session:
1. **Login** — Authenticates with credentials from a CSV file
2. **Portal Home** — Navigates to the home page
3. **Processes** — Opens the process list
4. **Task List** — Navigates to the task list dashboard
5. **Case List** — Navigates to the case list dashboard
6. **Logout** — Ends the session

This test validates response codes at every step, extracts dynamic values (ViewState, redirect URLs) via regex, and reports results as JTL and HTML.

### PerformancePortalTestReviewInGui

Same Portal walkthrough as above, but opens the **JMeter GUI** via `.showInGui()` for visual debugging and request/response inspection during local development.

## Setup

### Project Structure
```
<your_project_application>/
├── src_test/
│   └── <your_project_application>Test.java
├── resources/
│   ├── test.properties
│   └── <file_name>.csv
└── target/
    └── jtls/
```

### Quick Start
1. Clone the repository
2. Run the simple demo test:
```bash
mvn clean test -Dtest=DemoTest
```
3. To run `PerformancePortalTest`:
   - Open the code with Axon Ivy Designer
   - Install Portal from Axon Ivy Market (version must be compatible with Designer)
   - Update user credentials in `one_user.csv`
   - Run: `mvn clean test -Dtest=PerformancePortalTest`

### Configuration

Configure your test via `resources/test.properties`

### Properties Configuration

#### Server Properties
```properties
# Security System name
# Depends on server setup
# Keep it empty on local environment
security.system.name=

# Application name
application.name=designer

# Project name
project.name=<your_project_name>

# Server host
server.host=localhost
```

#### CSV Data Files
```properties
##### CSV file for user
one_user.csv=resources/<file_name>.csv
```

### CSV Data Configuration

CSV files contain user credentials in the format: `username,password`

Example (`users.csv`):
```csv
user1,passwordForUser1
```

CSV Data Set Configuration in Code:
```java
csvDataSet(csvFilePath)
  .variableNames("username,password")  // Define column names
  .delimiter(",")                      // CSV delimiter
  .ignoreFirstLine(false)              // Set to true if CSV has headers
```

### Credential Security Management

Your credential files contain sensitive information and should **never** be committed to version control. Here are several approaches to manage them securely:

> **Note:** This is just an example of how files could be used in a project. There are multiple ways to handle credentials; other solutions are possible.

**Jenkins Secret Files**
1. Go to Jenkins → Manage Jenkins → Manage Credentials
2. Add credentials of type "Secret file" for each CSV file
3. In your Jenkinsfile:
```groovy
pipeline {
  agent any
  stages {
    stage('Setup Credentials') {
      steps {
        script {
          withCredentials([
            file(credentialsId: 'your_credentials', variable: 'YOUR_CREDENTIALS_CSV'),
          ]) {
            sh '''
              cp "$YOUR_CREDENTIALS_CSV" "$<path_to_your_csv_file>"
            '''
          }
        }
      }
    }
    stage('Test') {
      steps {
        bat 'mvn test'
      }
    }
  }
}
```

**Security Best Practices**
1. **Never commit actual credentials** to version control
2. **Use template files** to document expected format
3. **Restrict access** to credential files on Jenkins server
4. **Rotate credentials** regularly
5. **Use least privilege** principle for test accounts

## HTTP Samplers Examples

### Basic HTTP Sampler
```java
httpSampler("ProjectStart",
  "/${__P(security.system.name)}/${__P(application.name)}/pro/${__P(project.name)}/1549F58C18A6C562/DefaultApplicationHomePage.ivp")
  .method("GET")
```

### HTTP Sampler with Parameters
```java
httpSampler("Login", "${url}")
  .method("POST")
  .param("javax.faces.partial.ajax", "true")
  .param("javax.faces.source", "login-form:login-command")
  .param("login:login-form:username", "${username}")
  .param("login:login-form:password", "${password}")
  .param("javax.faces.ViewState", "${viewState}")
```

## Passing Values and Variables

### Using properties from test.properties file
Use the `${__P(property.name)}` syntax to reference properties:
```java
.host("${__P(server.host)}") // Gets server.host value from the file
```

### Using Variables from CSV Data
Reference CSV column names as variables:
```java
.param("login:login-form:username", "${username}")  // From csvDataSet variableNames
.param("login:login-form:password", "${password}")  // From csvDataSet variableNames
```

### Using Extracted Variables
Variables extracted by regex extractors can be used in subsequent requests:
```java
httpSampler("Login", "${url}")  // Uses extracted 'url' variable
  .param("javax.faces.ViewState", "${viewState}")  // Uses extracted 'viewState' variable
```

## Regular Expression Extractors

Extractors capture values from HTTP responses for use in subsequent requests:

### Basic Regex Extractor
```java
.children(
  regexExtractor("url", "action=\"([^\"]+)\""),  // Extract form action URL
  regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\") ")  // Extract ViewState
)
```

### Redirect URL Extractor
```java
.children(
  regexExtractor("redirectURL", "<redirect url=\"([^\"]+)\">")  // Extract redirect URL from response
)
```

### Common Regex Patterns
```java
// Extract URLs from action attributes
regexExtractor("url", "action=\"([^\"]+)\"")

// Extract ViewState from JSF pages
regexExtractor("viewState", "id=\"j_id__v_0:javax.faces.ViewState:1\" value=(\"[\\S]+\") ")

// Extract redirect URLs from XML responses
regexExtractor("redirectURL", "<redirect url=\"([^\"]+)\">")
```

## Response Assertions

Response assertions validate that HTTP requests return expected results:

### Response Code Assertion
```java
.children(
  responseAssertion().fieldToTest(TargetField.RESPONSE_CODE).equalsToStrings("200")
)
```

### Response Body Assertion
```java
.children(
  responseAssertion().fieldToTest(TargetField.RESPONSE_DATA).containsSubstrings("Success")
)
```

### Available Target Fields
- `TargetField.RESPONSE_CODE`: HTTP status code (200, 404, 500, etc.)
- `TargetField.RESPONSE_DATA`: Response body content
- `TargetField.RESPONSE_HEADERS`: HTTP response headers
- `TargetField.RESPONSE_MESSAGE`: HTTP response message

### Assertion Methods
- `.equalsToStrings(value)`: Exact match
- `.containsSubstrings(value)`: Contains substring
- `.matchesRegex(pattern)`: Matches regular expression
- `.notContainsSubstrings(value)`: Does not contain substring

## Test Execution Configuration

### Thread Group Configuration
```java
threadGroup("test_name")
  .rampTo(numberOfUsers, Duration.ofSeconds(rampUpPeriod))
  .holdIterating(1)
```

### HTTP Defaults
```java
httpDefaults()
  .host("${__P(server.host)}")  // Default host for all requests
  .port(8081) // Default port for all requests
```

### HTTP Headers
```java
httpHeaders()
  .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9")
  .header("Accept-Encoding", "gzip, deflate, br")
  .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
```

### Cookies Management
```java
httpCookies()  // Enables automatic cookie handling
```

## Reporting and Results

### JTL Writer (Raw Results)
```java
jtlWriter(jtlDirName, testName + ".jtl")  // Saves raw test results
```

### Results Tree Visualizer (for debugging)
```java
resultsTreeVisualizer()  // Uncomment for local debugging only
```

## Troubleshooting

### Common Issues
1. **401/403 Errors**: Check user credentials in CSV files
2. **ViewState Errors**: Ensure ViewState extraction regex is correct
3. **Timeout Issues**: Adjust response time expectations in properties
4. **Connection Errors**: Verify server host and port configuration

### Debug Mode
Uncomment `resultsTreeVisualizer()` for detailed request/response inspection during local development. (see `PerformancePortalTest`)
