// package com.newapp.Erpnext.services;



// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.newapp.Erpnext.models.Employee;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpMethod;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.HttpClientErrorException;
// import org.springframework.web.client.HttpServerErrorException;
// import org.springframework.web.client.RestClientException;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.web.context.request.RequestContextHolder;
// import org.springframework.web.context.request.ServletRequestAttributes;

// import jakarta.servlet.http.HttpSession;
// import java.time.LocalDate;
// import java.time.format.DateTimeFormatter;
// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// @Service
// public class EmployeeCrudService {

//     private static final Logger logger = LoggerFactory.getLogger(EmployeeService.class);

//     @Autowired
//     private RestTemplate restTemplate;

//     @Autowired
//     private SessionService sessionService;

//     @Autowired
//     private ObjectMapper objectMapper;

//     @Value("${erpnext.base.url:http://erpnext.localhost:8000/api/resource/}")
//     private String erpnextBaseUrl;

//     @Value("${erpnext.base.url:http://erpnext.localhost:8000/api/}")
//     private String baseUrl;

//     private HttpHeaders createHeaders(String sid) {
//         HttpHeaders headers = new HttpHeaders();
//         headers.add("Cookie", "sid=" + sid);
//         headers.setContentType(MediaType.APPLICATION_JSON);
//         headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//         return headers;
//     }

//     // CREATE: Create a new Employee
//     public Employee createEmployee(Employee employee) throws Exception {
//         logger.info("Starting creation of Employee: {}", employee.getEmployeeName());

//         if (employee.getEmployeeName() == null || employee.getEmployeeName().trim().isEmpty()) {
//             logger.error("Employee name is required");
//             throw new IllegalArgumentException("Employee name cannot be empty");
//         }

//         HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
//                 .getRequest().getSession();
//         String sid = sessionService.ensureImportSessionValid(session);

//         String url = erpnextBaseUrl + "Employee";
//         HttpHeaders headers = createHeaders(sid);

//         Map<String, Object> requestBody = new HashMap<>();
//         requestBody.put("employee_name", employee.getEmployeeName());
//         requestBody.put("designation", employee.getDesignation());
//         requestBody.put("department", employee.getDepartment());
//         requestBody.put("company", employee.getCompany() != null ? employee.getCompany() : "Orinasa SA");
//         if (employee.getDateOfJoining() != null) {
//             requestBody.put("date_of_joining", employee.getDateOfJoining().format(DateTimeFormatter.ISO_LOCAL_DATE));
//         }

//         HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

//         try {
//             logger.debug("Sending POST request to: {}", url);
//             ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

//             if (response.getStatusCode().is2xxSuccessful()) {
//                 Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
//                 logger.debug("API response: {}", responseData);

//                 Employee createdEmployee = new Employee();
//                 createdEmployee.setName((String) responseData.get("name"));
//                 createdEmployee.setEmployeeName((String) responseData.get("employee_name"));
//                 createdEmployee.setDesignation((String) responseData.get("designation"));
//                 createdEmployee.setDepartment((String) responseData.get("department"));
//                 createdEmployee.setCompany((String) responseData.get("company"));
//                 String doj = (String) responseData.get("date_of_joining");
//                 if (doj != null) {
//                     createdEmployee.setDateOfJoining(LocalDate.parse(doj, DateTimeFormatter.ISO_LOCAL_DATE));
//                 }

//                 logger.info("Employee created successfully: ID={}", createdEmployee.getName());
//                 return createdEmployee;
//             } else {
//                 logger.error("Failed to create Employee - HTTP Code: {} | Response: {}", 
//                              response.getStatusCode(), response.getBody());
//                 throw new Exception("API error: " + response.getBody());
//             }
//         } catch (HttpClientErrorException e) {
//             logger.error("Client error during Employee creation: {}", e.getResponseBodyAsString());
//             throw new Exception("Client error: " + e.getResponseBodyAsString(), e);
//         } catch (HttpServerErrorException e) {
//             logger.error("Server error during Employee creation: {}", e.getResponseBodyAsString());
//             throw new Exception("Server error: " + e.getResponseBodyAsString(), e);
//         } catch (RestClientException e) {
//             logger.error("API communication error: {}", e.getMessage());
//             throw new Exception("API communication error: " + e.getMessage(), e);
//         }
//     }

//     // READ: Retrieve a single Employee by ID
//     public Employee getEmployeeById(String employeeId) throws Exception {
//         logger.info("Retrieving Employee with ID: {}", employeeId);

//         if (employeeId == null || employeeId.trim().isEmpty()) {
//             logger.error("Employee ID is required");
//             throw new IllegalArgumentException("Employee ID cannot be empty");
//         }

//         HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
//                 .getRequest().getSession();
//         String sid = sessionService.ensureImportSessionValid(session);

//         String url = erpnextBaseUrl + "Employee/" + employeeId;
//         HttpHeaders headers = createHeaders(sid);
//         HttpEntity<String> entity = new HttpEntity<>(headers);

//         try {
//             logger.debug("Sending GET request to: {}", url);
//             ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

//             if (response.getStatusCode().is2xxSuccessful()) {
//                 Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
//                 logger.debug("Retrieved Employee data: {}", responseData);

//                 Employee employee = new Employee();
//                 employee.setName((String) responseData.get("name"));
//                 employee.setEmployeeName((String) responseData.get("employee_name"));
//                 employee.setDesignation((String) responseData.get("designation"));
//                 employee.setDepartment((String) responseData.get("department"));
//                 employee.setCompany((String) responseData.get("company"));
//                 String doj = (String) responseData.get("date_of_joining");
//                 if (doj != null) {
//                     employee.setDateOfJoining(LocalDate.parse(doj, DateTimeFormatter.ISO_LOCAL_DATE));
//                 }

//                 logger.info("Employee retrieved successfully: ID={}", employee.getName());
//                 return employee;
//             } else {
//                 logger.error("Failed to retrieve Employee - HTTP Code: {} | Response: {}", 
//                              response.getStatusCode(), response.getBody());
//                 throw new Exception("API error: " + response.getBody());
//             }
//         } catch (HttpClientErrorException e) {
//             logger.error("Client error during Employee retrieval: {}", e.getResponseBodyAsString());
//             throw new Exception("Client error: " + e.getResponseBodyAsString(), e);
//         } catch (HttpServerErrorException e) {
//             logger.error("Server error during Employee retrieval: {}", e.getResponseBodyAsString());
//             throw new Exception("Server error: " + e.getResponseBodyAsString(), e);
//         }
//     }

//     // READ: Retrieve all Employees
//     public List<Employee> getAllEmployees() throws Exception {
//         logger.info("Fetching all Employees");

//         HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
//                 .getRequest().getSession();
//         String sid = sessionService.ensureImportSessionValid(session);

//         String url = erpnextBaseUrl + "Employee?fields=[\"name\",\"employee_name\",\"designation\",\"department\",\"date_of_joining\",\"company\"]";
//         HttpHeaders headers = createHeaders(sid);
//         HttpEntity<String> entity = new HttpEntity<>(headers);

//         try {
//             logger.debug("Sending GET request to: {}", url);
//             ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

//             List<Employee> employees = new ArrayList<>();
//             if (response.getStatusCode().is2xxSuccessful()) {
//                 Map<String, Object> responseBody = response.getBody();
//                 List<Map<String, Object>> data = (List<Map<String, Object>>) responseBody.get("data");

//                 for (Map<String, Object> item : data) {
//                     Employee employee = new Employee();
//                     employee.setName((String) item.get("name"));
//                     employee.setEmployeeName((String) item.get("employee_name"));
//                     employee.setDesignation((String) item.get("designation"));
//                     employee.setDepartment((String) item.get("department"));
//                     employee.setCompany((String) item.get("company"));
//                     String doj = (String) item.get("date_of_joining");
//                     if (doj != null) {
//                         employee.setDateOfJoining(LocalDate.parse(doj, DateTimeFormatter.ISO_LOCAL_DATE));
//                     }
//                     employees.add(employee);
//                 }

//                 logger.info("Retrieved {} employees", employees.size());
//                 return employees;
//             } else {
//                 logger.error("Failed to retrieve Employees - HTTP Code: {} | Response: {}", 
//                              response.getStatusCode(), response.getBody());
//                 throw new Exception("API error: " + response.getBody());
//             }
//         } catch (HttpClientErrorException e) {
//             logger.error("Client error during Employees retrieval: {}", e.getResponseBodyAsString());
//             throw new Exception("Client error: " + e.getResponseBodyAsString(), e);
//         } catch (HttpServerErrorException e) {
//             logger.error("Server error during Employees retrieval: {}", e.getResponseBodyAsString());
//             throw new Exception("Server error: " + e.getResponseBodyAsString(), e);
//         }
//     }

//     // UPDATE: Update an existing Employee
//     public Employee updateEmployee(Employee employee) throws Exception {
//         logger.info("Starting update of Employee: ID={}", employee.getName());

//         if (employee.getName() == null || employee.getName().trim().isEmpty()) {
//             logger.error("Employee ID is required for update");
//             throw new IllegalArgumentException("Employee ID cannot be empty");
//         }

//         HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
//                 .getRequest().getSession();
//         String sid = sessionService.ensureImportSessionValid(session);

//         String url = erpnextBaseUrl + "Employee/" + employee.getName();
//         HttpHeaders headers = createHeaders(sid);

//         Map<String, Object> requestBody = new HashMap<>();
//         requestBody.put("employee_name", employee.getEmployeeName());
//         requestBody.put("designation", employee.getDesignation());
//         requestBody.put("department", employee.getDepartment());
//         requestBody.put("company", employee.getCompany());
//         if (employee.getDateOfJoining() != null) {
//             requestBody.put("date_of_joining", employee.getDateOfJoining().format(DateTimeFormatter.ISO_LOCAL_DATE));
//         }

//         HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

//         try {
//             logger.debug("Sending PUT request to: {}", url);
//             ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, entity, Map.class);

//             if (response.getStatusCode().is2xxSuccessful()) {
//                 Map<String, Object> responseData = (Map<String, Object>) response.getBody().get("data");
//                 logger.debug("API response: {}", responseData);

//                 Employee updatedEmployee = new Employee();
//                 updatedEmployee.setName((String) responseData.get("name"));
//                 updatedEmployee.setEmployeeName((String) responseData.get("employee_name"));
//                 updatedEmployee.setDesignation((String) responseData.get("designation"));
//                 updatedEmployee.setDepartment((String) responseData.get("department"));
//                 updatedEmployee.setCompany((String) responseData.get("company"));
//                 String doj = (String) responseData.get("date_of_joining");
//                 if (doj != null) {
//                     updatedEmployee.setDateOfJoining(LocalDate.parse(doj, DateTimeFormatter.ISO_LOCAL_DATE));
//                 }

//                 logger.info("Employee updated successfully: ID={}", updatedEmployee.getName());
//                 return updatedEmployee;
//             } else {
//                 logger.error("Failed to update Employee - HTTP Code: {} | Response: {}", 
//                              response.getStatusCode(), response.getBody());
//                 throw new Exception("API error: " + response.getBody());
//             }
//         } catch (HttpClientErrorException e) {
//             logger.error("Client error during Employee update: {}", e.getResponseBodyAsString());
//             throw new Exception("Client error: " + e.getResponseBodyAsString(), e);
//         } catch (HttpServerErrorException e) {
//             logger.error("Server error during Employee update: {}", e.getResponseBodyAsString());
//             throw new Exception("Server error: " + e.getResponseBodyAsString(), e);
//         }
//     }

//     // DELETE: Cancel an Employee (soft delete via Frappe's cancel method)
//     public boolean cancelEmployee(String employeeId) throws Exception {
//         logger.info("Starting cancellation of Employee: ID={}", employeeId);

//         if (employeeId == null || employeeId.trim().isEmpty()) {
//             logger.error("Employee ID is required for cancellation");
//             throw new IllegalArgumentException("Employee ID cannot be empty");
//         }

//         HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
//                 .getRequest().getSession();
//         String sid = sessionService.ensureImportSessionValid(session);

//         String url = baseUrl + "method/frappe.client.cancel";
//         HttpHeaders headers = createHeaders(sid);
//         headers.setContentType(MediaType.APPLICATION_JSON);

//         Map<String, String> params = new HashMap<>();
//         params.put("doctype", "Employee");
//         params.put("name", employeeId);

//         HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(params), headers);

//         try {
//             logger.debug("Sending POST request to cancel Employee: {}", url);
//             ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

//             if (response.getStatusCode().is2xxSuccessful()) {
//                 logger.info("Employee cancelled successfully: ID={}", employeeId);
//                 return true;
//             } else {
//                 logger.error("Failed to cancel Employee - HTTP Code: {} | Response: {}", 
//                              response.getStatusCode(), response.getBody());
//                 throw new Exception("Failed to cancel Employee: " + response.getBody());
//             }
//         } catch (HttpClientErrorException e) {
//             logger.error("Client error during Employee cancellation: {}", e.getResponseBodyAsString());
//             throw new Exception("Client error: " + e.getResponseBodyAsString(), e);
//         } catch (HttpServerErrorException e) {
//             logger.error("Server error during Employee cancellation: {}", e.getResponseBodyAsString());
//             throw new Exception("Server error: " + e.getResponseBodyAsString(), e);
//         }
//     }

//     // DELETE: Hard delete an Employee
//     public boolean deleteEmployee(String employeeId) throws Exception {
//         logger.info("Starting deletion of Employee: ID={}", employeeId);

//         if (employeeId == null || employeeId.trim().isEmpty()) {
//             logger.error("Employee ID is required for deletion");
//             throw new IllegalArgumentException("Employee ID cannot be empty");
//         }

//         HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
//                 .getRequest().getSession();
//         String sid = sessionService.ensureImportSessionValid(session);

//         String url = erpnextBaseUrl + "Employee/" + employeeId;
//         HttpHeaders headers = createHeaders(sid);
//         HttpEntity<String> entity = new HttpEntity<>(headers);

//         try {
//             logger.debug("Sending DELETE request to: {}", url);
//             ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, Map.class);

//             if (response.getStatusCode().is2xxSuccessful()) {
//                 logger.info("Employee deleted successfully: ID={}", employeeId);
//                 return true;
//             } else {
//                 logger.error("Failed to delete Employee - HTTP Code: {} | Response: {}", 
//                              response.getStatusCode(), response.getBody());
//                 throw new Exception("Failed to delete Employee: " + response.getBody());
//             }
//         } catch (HttpClientErrorException e) {
//             logger.error("Client error during Employee deletion: {}", e.getResponseBodyAsString());
//             throw new Exception("Client error: " + e.getResponseBodyAsString(), e);
//         } catch (HttpServerErrorException e) {
//             logger.error("Server error during Employee deletion: {}", e.getResponseBodyAsString());
//             throw new Exception("Server error: " + e.getResponseBodyAsString(), e);
//         }
//     }
// } {
    
// }
