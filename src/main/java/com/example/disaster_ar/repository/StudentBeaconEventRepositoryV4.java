package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.StudentBeaconEventV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentBeaconEventRepositoryV4 extends JpaRepository<StudentBeaconEventV4, String> {
    Optional<StudentBeaconEventV4> findTopByScenario_IdAndStudent_IdOrderByEventAtDesc(
            String scenarioId,
            String studentId
    );
}