package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, String> {
    Optional<School> findBySchoolName(String schoolName);

    // ★ 학교 코드로 찾기
    Optional<School> findByAccessCode(String accessCode);
}
