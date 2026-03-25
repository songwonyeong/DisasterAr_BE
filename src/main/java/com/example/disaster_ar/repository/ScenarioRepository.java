package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.ScenarioV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScenarioRepository extends JpaRepository<ScenarioV4, String> {
    List<ScenarioV4> findByClassroom_IdOrderByCreatedTimeDesc(String classroomId);
}