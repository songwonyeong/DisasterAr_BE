package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.domain.v4.UserV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.domain.v4.enums.TrainingState;
import com.example.disaster_ar.dto.room.RoomCreateRequest;
import com.example.disaster_ar.dto.room.RoomResponse;
import com.example.disaster_ar.dto.room.RoomUpdateRequest;
import com.example.disaster_ar.dto.room.TrainingStatusResponse;
import com.example.disaster_ar.repository.ClassroomRepository;
import com.example.disaster_ar.repository.SchoolRepository;
import com.example.disaster_ar.repository.UserRepository;
import com.example.disaster_ar.repository.StudentRepositoryV4;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final ClassroomRepository classroomRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final StudentRepositoryV4 studentRepository;

    public RoomResponse createRoom(RoomCreateRequest req) {

        SchoolV4 school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        UserV4 owner = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        ClassroomV4 c = new ClassroomV4();
        if (c.getId() == null) c.setId(UUID.randomUUID().toString());

        c.setSchool(school);
        c.setOwner(owner); // ✅ 수정
        c.setClassName(req.getClassName());
        c.setStudentCount(0);
        c.setJoinCode(generateJoinCode());
        c.setActiveMapVersion(null); // ✅ 수정
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        c.setTrainingState(TrainingState.WAITING);
        c.setTrainingStartedAt(null);
        c.setTrainingEndedAt(null);
        c.setActiveScenario(null);

        classroomRepository.save(c);
        return toDto(c);
    }

    public List<RoomResponse> listBySchool(String schoolId) {
        SchoolV4 school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        return classroomRepository.findBySchool(school).stream()
                .map(this::toDto)
                .toList();
    }

    public RoomResponse updateRoom(RoomUpdateRequest req) {
        ClassroomV4 c = classroomRepository.findById(req.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (c.getOwner() == null || !c.getOwner().getId().equals(req.getUserId())) { // ✅ 수정
            throw new IllegalArgumentException("이 방을 수정할 권한이 없습니다.");
        }

        if (req.getClassName() != null) c.setClassName(req.getClassName());
        if (req.getStudentCount() != null) c.setStudentCount(req.getStudentCount());

        c.setUpdatedAt(LocalDateTime.now());
        ClassroomV4 saved = classroomRepository.save(c);
        return toDto(saved);
    }

    public void deleteRoom(String classroomId, String userId) {
        ClassroomV4 c = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (c.getOwner() == null || !c.getOwner().getId().equals(userId)) { // ✅ 수정
            throw new IllegalArgumentException("이 방을 삭제할 권한이 없습니다.");
        }

        classroomRepository.delete(c);
    }

    public RoomResponse regenerateJoinCode(String classroomId, String userId) {
        ClassroomV4 c = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (c.getOwner() == null || !c.getOwner().getId().equals(userId)) { // ✅ 수정
            throw new IllegalArgumentException("이 방의 코드를 재발급할 권한이 없습니다.");
        }

        c.setJoinCode(generateJoinCode());
        c.setUpdatedAt(LocalDateTime.now());
        ClassroomV4 saved = classroomRepository.save(c);
        return toDto(saved);
    }

    private RoomResponse toDto(ClassroomV4 c) {
        return RoomResponse.builder()
                .classroomId(c.getId())
                .schoolId(c.getSchool().getId())
                .className(c.getClassName())
                .studentCount(c.getStudentCount())
                .joinCode(c.getJoinCode())
                .trainingState(c.getTrainingState() != null ? c.getTrainingState().name() : null)
                .build();
    }
    private String generateJoinCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public TrainingStatusResponse getTrainingStatus(String classroomId, String studentId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        StudentV4 student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        if (!student.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 학생은 이 교실 소속이 아닙니다.");
        }

        return TrainingStatusResponse.builder()
                .classroomId(classroom.getId())
                .studentId(student.getId())
                .trainingState(
                        classroom.getTrainingState() != null
                                ? classroom.getTrainingState().name()
                                : null
                )
                .trainingStartedAt(classroom.getTrainingStartedAt())
                .trainingEndedAt(classroom.getTrainingEndedAt())
                .activeScenarioId(
                        classroom.getActiveScenario() != null
                                ? classroom.getActiveScenario().getId()
                                : null
                )
                .isKicked(Boolean.TRUE.equals(student.getIsKicked()))
                .build();
    }
}