package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ScenarioAssignmentV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioAssignmentRepositoryV4 extends JpaRepository<ScenarioAssignmentV4, String> {
    List<ScenarioAssignmentV4> findByScenario_IdAndBeacon_Id(String scenarioId, String beaconId);

    List<ScenarioAssignmentV4> findByScenario_IdAndClassroom_IdAndBeacon_Id(
            String scenarioId,
            String classroomId,
            String beaconId
    );

    List<ScenarioAssignmentV4> findByScenario_IdOrderByCreatedAtAsc(String scenarioId);

    List<ScenarioAssignmentV4> findByScenario_IdAndClassroom_IdAndElementId(
            String scenarioId,
            String classroomId,
            String elementId
    );
}