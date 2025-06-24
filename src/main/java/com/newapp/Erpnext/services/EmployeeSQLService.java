package com.newapp.Erpnext.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import com.newapp.Erpnext.entity.EmployeeEntity;
import com.newapp.Erpnext.repository.EmployeeRepository;

@Service
public class EmployeeSQLService {
    
    @Autowired
    private EmployeeRepository employeeRepository;

    // ================================================================
    // CREATE - Créer un nouvel employé
    // ================================================================
    
    public EmployeeEntity createEmployee(EmployeeEntity employee) {
        // Validation métier si nécessaire
        if (employee.getName() == null || employee.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'employé est obligatoire");
        }
        
        // Vérifier si l'employé existe déjà
        if (employeeRepository.existsById(employee.getName())) {
            throw new IllegalArgumentException("Un employé avec ce nom existe déjà");
        }
        
        return employeeRepository.save(employee);
    }
    
    public EmployeeEntity saveEmployee(EmployeeEntity employee) {
        return employeeRepository.save(employee);
    }
    
    // ================================================================
    // READ - Lire les données
    // ================================================================
    
    // Récupérer tous les employés
    public List<EmployeeEntity> getAllEmployees() {
        return employeeRepository.findAll();
    }
    
    // Récupérer un employé par ID
    public Optional<EmployeeEntity> getEmployeeById(String id) {
        return employeeRepository.findById(id);
    }
    
    // Récupérer un employé par ID (avec exception si non trouvé)
    public EmployeeEntity getEmployeeByIdRequired(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employé non trouvé avec l'ID: " + id));
    }
    
    // Recherches personnalisées
    // public List<EmployeeEntity> getEmployeesByStatus(String status) {
    //     return employeeRepository.findByStatus(status);
    // }
    
    // public List<EmployeeEntity> searchEmployeesByName(String name) {
    //     return employeeRepository.findByEmployeeNameContaining(name);
    // }
    
    // public List<EmployeeEntity> getEmployeesByDepartment(String department) {
    //     return employeeRepository.findByDepartment(department);
    // }
    
    // public List<EmployeeEntity> getEmployeesByCompanyAndStatus(String company, String status) {
    //     return employeeRepository.findByCompanyAndStatus(company, status);
    // }
    
    // Compter les employés
    public long countAllEmployees() {
        return employeeRepository.count();
    }
    
    // public long countEmployeesByStatus(String status) {
    //     return employeeRepository.findByStatus(status).size();
    // }
    
    // ================================================================
    // UPDATE - Mettre à jour
    // ================================================================
    
    public EmployeeEntity updateEmployee(String id, EmployeeEntity employeeDetails) {
        EmployeeEntity existingEmployee = getEmployeeByIdRequired(id);
        
        // Mettre à jour les champs
        if (employeeDetails.getEmployeeName() != null) {
            existingEmployee.setEmployeeName(employeeDetails.getEmployeeName());
        }
        // Ajoutez d'autres champs selon vos besoins
        
        return employeeRepository.save(existingEmployee);
    }
    
    public EmployeeEntity updateEmployeePartial(String id, EmployeeEntity updates) {
        EmployeeEntity existing = getEmployeeByIdRequired(id);
        
        // Mise à jour partielle - ne modifie que les champs non null
        if (updates.getEmployeeName() != null) {
            existing.setEmployeeName(updates.getEmployeeName());
        }
        // Ajoutez d'autres champs...
        
        return employeeRepository.save(existing);
    }
    
    // ================================================================
    // DELETE - Supprimer
    // ================================================================
    
    public void deleteEmployee(String id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Employé non trouvé avec l'ID: " + id);
        }
        employeeRepository.deleteById(id);
    }
    
    public void deleteEmployeeEntity(EmployeeEntity employee) {
        employeeRepository.delete(employee);
    }
    
    public void deleteAllEmployees() {
        employeeRepository.deleteAll();
    }
    
    // Supprimer par critères
    // public void deleteEmployeesByStatus(String status) {
    //     List<EmployeeEntity> employees = employeeRepository.findByStatus(status);
    //     employeeRepository.deleteAll(employees);
    // }
    
    // ================================================================
    // MÉTHODES UTILITAIRES
    // ================================================================
    
    public boolean employeeExists(String id) {
        return employeeRepository.existsById(id);
    }
    
    public List<EmployeeEntity> saveAllEmployees(List<EmployeeEntity> employees) {
        return employeeRepository.saveAll(employees);
    }
}

