package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.TeamMissionStepProgressV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamMissionStepProgressRepositoryV4 extends JpaRepository<TeamMissionStepProgressV4, String> {

    Optional<TeamMissionStepProgressV4>
    findByScenario_IdAndAssignment_IdAndTeam_IdAndStepOrder(
            String scenarioId,
            String assignmentId,
            String teamId,
            Integer stepOrder
    );

    List<TeamMissionStepProgressV4>
    findByScenario_IdAndAssignment_IdAndTeam_IdOrderByStepOrderAsc(
            String scenarioId,
            String assignmentId,
            String teamId
    );
}