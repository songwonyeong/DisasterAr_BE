package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.StudentV4;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepositoryV4 extends JpaRepository<StudentV4, String> {
    java.util.List<StudentV4> findByClassroom_IdOrderByJoinedAtAsc(String classroomId);

    List<StudentV4> findByClassroom_IdAndIsKickedFalseOrderByJoinedAtAsc(String classroomId);

    List<StudentV4> findByClassroom_IdAndIsKickedFalseAndTrainingSessionIdIsNullOrderByJoinedAtAsc(String classroomId);

    List<StudentV4> findByClassroom_IdAndIsKickedFalseAndTrainingSessionIdOrderByJoinedAtAsc(
            String classroomId,
            String trainingSessionId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    update StudentV4 s
       set s.lastBeacon = null,
           s.lastBeaconRssi = null,
           s.lastBeaconSeenAt = null
     where s.lastBeacon.id = :beaconId
""")
    int clearLastBeaconByBeaconId(@Param("beaconId") String beaconId);
}
