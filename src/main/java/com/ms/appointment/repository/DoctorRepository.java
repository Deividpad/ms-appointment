package com.ms.appointment.repository;

import com.ms.appointment.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}

