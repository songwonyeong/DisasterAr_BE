package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<ClassroomV4, String> {
    Optional<ClassroomV4> findByJoinCode(String joinCode);
    List<ClassroomV4> findBySchool(SchoolV4 school);
    @Modifying
    @Query("""
    update ClassroomV4 c
    set c.activeScenario = null
    where c.activeScenario.id = :scenarioId
""")
    void clearActiveScenarioByScenarioId(@Param("scenarioId") String scenarioId);
}