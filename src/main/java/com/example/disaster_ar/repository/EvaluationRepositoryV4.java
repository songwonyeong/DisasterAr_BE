package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.EvaluationV4;
import com.example.disaster_ar.domain.v4.enums.EvalLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRepositoryV4 extends JpaRepository<EvaluationV4, String> {

    Optional<EvaluationV4> findTopByScenario_IdAndLevelOrderByCreatedAtDesc(
            String scenarioId,
            EvalLevel level
    );

    List<EvaluationV4> findByScenario_IdAndLevelOrderByCreatedAtDesc(
            String scenarioId,
            EvalLevel level
    );
}