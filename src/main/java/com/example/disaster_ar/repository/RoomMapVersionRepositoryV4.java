package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.RoomMapVersionV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomMapVersionRepositoryV4 extends JpaRepository<RoomMapVersionV4, String> {
    List<RoomMapVersionV4> findByClassroom_IdOrderByCreatedAtDesc(String classroomId);
}