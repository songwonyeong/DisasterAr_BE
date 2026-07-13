package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.StudentCallMissionV4;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentCallMissionRepositoryV4 extends JpaRepository<StudentCallMissionV4, String> {

    Optional<StudentCallMissionV4> findByScenario_IdAndTrainingSessionIdAndStudent_Id(
            String scenarioId,
            String trainingSessionId,
            String studentId
    );

    List<StudentCallMissionV4> findByScenario_IdAndTrainingSessionIdOrderByTriggerOffsetSecondsAsc(
            String scenarioId,
            String trainingSessionId
    );

    void deleteByScenario_Id(String scenarioId);

    @Modifying
    @Query("delete from StudentCallMissionV4 m where m.classroom.id = :classroomId")
    void deleteByClassroomId(@Param("classroomId") String classroomId);
}
