package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ScenarioTeamV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScenarioTeamRepositoryV4 extends JpaRepository<ScenarioTeamV4, String> {

    List<ScenarioTeamV4> findByScenario_IdOrderByTeamCodeAsc(String scenarioId);

    Optional<ScenarioTeamV4> findByScenario_IdAndTeamCode(String scenarioId, String teamCode);
}