package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.TeamMissionProgressV4;
import com.example.disaster_ar.domain.v4.enums.ProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamMissionProgressRepositoryV4 extends JpaRepository<TeamMissionProgressV4, String> {

    // 일반 조회용: active-assignments, fireteam/state에서 사용
    Optional<TeamMissionProgressV4> findByScenario_IdAndAssignment_IdAndTeam_Id(
            String scenarioId,
            String assignmentId,
            String teamId
    );

    // 도넛 progress 증가용: 트랜잭션 안에서만 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select p
        from TeamMissionProgressV4 p
        where p.scenario.id = :scenarioId
          and p.assignment.id = :assignmentId
          and p.team.id = :teamId
    """)
    Optional<TeamMissionProgressV4> findForUpdateByScenarioIdAndAssignmentIdAndTeamId(
            @Param("scenarioId") String scenarioId,
            @Param("assignmentId") String assignmentId,
            @Param("teamId") String teamId
    );

    List<TeamMissionProgressV4> findByScenario_IdAndTeam_IdAndStatus(
            String scenarioId,
            String teamId,
            ProgressStatus status
    );

    void deleteByScenario_Id(String scenarioId);

}