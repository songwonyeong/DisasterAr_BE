package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.Classroom;
import com.example.disaster_ar.domain.School;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, String> {

    // ★ joinCode로 찾기 (기존)
    Optional<Classroom> findByJoinCode(String joinCode);

    // ★ 학교(채널)로 방 목록 조회 (RoomService에서 사용)
    List<Classroom> findBySchool(School school);
}
