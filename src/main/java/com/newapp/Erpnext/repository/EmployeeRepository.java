package com.newapp.Erpnext.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.newapp.Erpnext.entity.EmployeeEntity;
import com.newapp.Erpnext.models.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<EmployeeEntity, String> {
    List<EmployeeEntity> findAll();
    @Override
    default Optional<EmployeeEntity> findById(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'findById'");
    }

    @Override
    default <S extends EmployeeEntity> S save(S entity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'save'");
    }
    
    @Query(value = "SELECT * FROM tabEmployee LIMIT 5", nativeQuery = true)
    List<EmployeeEntity> findFirst5();
    
    @Query(value = "SELECT COUNT(*) FROM tabEmployee", nativeQuery = true)
    Long countEmployees();
}
