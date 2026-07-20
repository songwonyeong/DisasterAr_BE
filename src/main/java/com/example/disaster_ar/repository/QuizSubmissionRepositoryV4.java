package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.QuizSubmissionV4;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface QuizSubmissionRepositoryV4 extends JpaRepository<QuizSubmissionV4, String> {

    @Query("""
        select q.content.id
        from QuizSubmissionV4 q
        where q.scenario.id = :scenarioId
          and q.assignment.id = :assignmentId
          and q.student.id = :studentId
          and q.content is not null
    """)
    List<String> findSubmittedContentIds(
            @Param("scenarioId") String scenarioId,
            @Param("assignmentId") String assignmentId,
            @Param("studentId") String studentId
    );

    Optional<QuizSubmissionV4> findByScenario_IdAndAssignment_IdAndStudent_IdAndContent_Id(
            String scenarioId,
            String assignmentId,
            String studentId,
            String contentId
    );

    List<QuizSubmissionV4> findByScenario_IdAndAssignment_IdAndStudent_Id(
            String scenarioId,
            String assignmentId,
            String studentId
    );

    long countByScenario_IdAndAssignment_IdAndStudent_Id(
            String scenarioId,
            String assignmentId,
            String studentId
    );

    long countByScenario_IdAndAssignment_IdAndStudent_IdAndIsCorrectTrue(
            String scenarioId,
            String assignmentId,
            String studentId
    );

    void deleteByScenario_Id(String scenarioId);

    List<QuizSubmissionV4> findByScenario_IdAndStudent_IdOrderBySubmittedAtAsc(
            String scenarioId,
            String studentId
    );

}