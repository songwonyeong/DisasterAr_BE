package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ScenarioActionEventV4;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScenarioActionEventRepositoryV4 extends JpaRepository<ScenarioActionEventV4, String> {

    /**
     * 랜덤 퀴즈 발급은 scenario_action_events에 QUIZ_TRIGGER로 기록한다.
     *
     * element_id = assignmentId
     * value_text = contentId
     *
     * 아직 quiz_submissions에 제출 row가 없는 contentId를 pending으로 본다.
     */
    @Query(value = """
        select e.value_text
        from scenario_action_events e
        where e.scenario_id = :scenarioId
          and e.student_id = :studentId
          and e.action_type = 'QUIZ_TRIGGER'
          and e.element_id = :assignmentId
          and e.value_text is not null
          and not exists (
              select 1
              from quiz_submissions qs
              where qs.scenario_id = e.scenario_id
                and qs.assignment_id = e.element_id
                and qs.student_id = e.student_id
                and qs.content_id = e.value_text
          )
        order by e.created_at desc
        limit 1
        """, nativeQuery = true)
    Optional<String> findPendingRandomQuizContentId(
            @Param("scenarioId") String scenarioId,
            @Param("assignmentId") String assignmentId,
            @Param("studentId") String studentId
    );

    /**
     * 이미 발급된 랜덤 퀴즈 contentId 전체.
     * 제출 여부와 관계없이 새 랜덤 후보에서 제외한다.
     */
    @Query(value = """
        select distinct e.value_text
        from scenario_action_events e
        where e.scenario_id = :scenarioId
          and e.student_id = :studentId
          and e.action_type = 'QUIZ_TRIGGER'
          and e.element_id = :assignmentId
          and e.value_text is not null
        """, nativeQuery = true)
    List<String> findIssuedRandomQuizContentIds(
            @Param("scenarioId") String scenarioId,
            @Param("assignmentId") String assignmentId,
            @Param("studentId") String studentId
    );

    boolean existsByBeacon_id(String beaconId);
    boolean existsByBeacon_Id(String beaconId);

    void deleteByScenario_Id(String scenarioId);
}