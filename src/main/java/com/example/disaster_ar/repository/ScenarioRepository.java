package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioRepository extends JpaRepository<Scenario, String> {

    // 기존에 String classroomId 필드 쓰던 거 대신
    List<Scenario> findByClassroom_IdOrderByCreatedTimeDesc(String classroomId);
}
