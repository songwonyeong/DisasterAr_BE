package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ScenarioTeamMemberV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioTeamMemberRepositoryV4 extends JpaRepository<ScenarioTeamMemberV4, String> {

    Optional<ScenarioTeamMemberV4> findByScenario_IdAndTeam_IdAndStudent_Id(
            String scenarioId,
            String teamId,
            String studentId
    );

    List<ScenarioTeamMemberV4> findByScenario_IdOrderByAssignedAtAsc(String scenarioId);

    void deleteByScenario_Id(String scenarioId);

    Optional<ScenarioTeamMemberV4> findByScenario_IdAndStudent_Id(
            String scenarioId,
            String studentId
    );

    boolean existsByScenario_Id(String scenarioId);
}