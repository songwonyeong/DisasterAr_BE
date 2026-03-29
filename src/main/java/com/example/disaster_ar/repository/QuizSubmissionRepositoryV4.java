package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.QuizSubmissionV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSubmissionRepositoryV4 extends JpaRepository<QuizSubmissionV4, String> {
    Optional<QuizSubmissionV4> findByScenario_IdAndAssignment_IdAndStudent_Id(
            String scenarioId,
            String assignmentId,
            String studentId
    );
}