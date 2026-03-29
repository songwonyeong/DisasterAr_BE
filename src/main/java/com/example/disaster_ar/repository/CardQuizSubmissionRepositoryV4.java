package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.CardQuizSubmissionV4;
import com.example.disaster_ar.domain.v4.enums.CardQuizKind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardQuizSubmissionRepositoryV4 extends JpaRepository<CardQuizSubmissionV4, String> {
    Optional<CardQuizSubmissionV4> findByScenario_IdAndAssignment_IdAndStudent_IdAndQuizKind(
            String scenarioId,
            String assignmentId,
            String studentId,
            CardQuizKind quizKind
    );
}