package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.TeamMissionProgressV4;
import com.example.disaster_ar.domain.v4.enums.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMissionProgressRepositoryV4 extends JpaRepository<TeamMissionProgressV4, String> {

    Optional<TeamMissionProgressV4>
    findByScenario_IdAndAssignment_IdAndTeam_Id(
            String scenarioId,
            String assignmentId,
            String teamId);

    List<TeamMissionProgressV4> findByScenario_IdAndTeam_IdAndStatus(
            String scenarioId,
            String teamId,
            ProgressStatus status
    );
}