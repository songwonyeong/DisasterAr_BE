package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.SchoolV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<SchoolV4, String> {
    Optional<SchoolV4> findBySchoolName(String schoolName);
    Optional<SchoolV4> findByAccessCode(String accessCode);
}