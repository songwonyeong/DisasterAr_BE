package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.StudentItemV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentItemRepositoryV4 extends JpaRepository<StudentItemV4, String> {

    Optional<StudentItemV4> findByScenario_IdAndStudent_IdAndItem_Id(
            String scenarioId,
            String studentId,
            String itemId
    );

    List<StudentItemV4> findByScenario_IdAndStudent_Id(String scenarioId, String studentId);
}