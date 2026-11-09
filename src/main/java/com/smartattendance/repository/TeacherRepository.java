package com.smartattendance.repository;

import com.smartattendance.entity.Teacher;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    Optional<Teacher> findByUser_Email(String email);

    boolean existsByEmployeeIdAndIdNot(
            String employeeId,
            Long id
    );
}
