package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.StudentMissionProgressV4;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface StudentMissionProgressRepositoryV4
        extends JpaRepository<StudentMissionProgressV4, String> {

    Optional<StudentMissionProgressV4>
    findByScenario_IdAndAssignment_IdAndStudent_Id(
            String scenarioId,
            String assignmentId,
            String studentId
    );

    void deleteByScenario_Id(String scenarioId);

    List<StudentMissionProgressV4> findByScenario_IdAndStudent_Id(
            String scenarioId,
            String studentId
    );
}