package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ScenarioTriggerV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioTriggerRepositoryV4 extends JpaRepository<ScenarioTriggerV4, String> {
    Optional<ScenarioTriggerV4> findByScenario_IdAndStudent_IdAndAssignment_Id(
            String scenarioId,
            String studentId,
            String assignmentId
    );

    List<ScenarioTriggerV4> findByScenario_IdAndStudent_IdOrderByTriggeredAtDesc(
            String scenarioId,
            String studentId
    );
}