package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.StudentV4;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentRepositoryV4 extends JpaRepository<StudentV4, String> {
    java.util.List<StudentV4> findByClassroom_IdOrderByJoinedAtAsc(String classroomId);

    List<StudentV4> findByClassroom_IdAndIsKickedFalseOrderByJoinedAtAsc(String classroomId);
}
