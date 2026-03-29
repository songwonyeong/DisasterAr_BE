package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.domain.v4.UserV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.domain.v4.enums.TrainingState;
import com.example.disaster_ar.dto.room.*;
import com.example.disaster_ar.repository.*;
import com.example.disaster_ar.domain.v4.RoomMapVersionV4;
import com.example.disaster_ar.domain.v4.ScenarioV4;
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
    private final ScenarioRepository scenarioRepository;
    private final RoomMapVersionRepositoryV4 roomMapVersionRepositoryV4;

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

    public RoomMapResponse getRoomMap(String classroomId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        RoomMapVersionV4 activeMapVersion = classroom.getActiveMapVersion();

        if (activeMapVersion == null) {
            throw new IllegalArgumentException("이 교실의 활성 구조도가 존재하지 않습니다.");
        }

        return RoomMapResponse.builder()
                .mapVersionId(activeMapVersion.getId())
                .classroomId(classroom.getId())
                .schoolId(classroom.getSchool() != null ? classroom.getSchool().getId() : null)
                .label(activeMapVersion.getLabel())
                .floorsJson(activeMapVersion.getFloorsJson())
                .createdAt(activeMapVersion.getCreatedAt())
                .build();
    }

    public TrainingControlResponse startTraining(String classroomId, TrainingStartRequest req) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        ScenarioV4 scenario = scenarioRepository.findById(req.getScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (scenario.getClassroom() == null || !scenario.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 시나리오는 이 교실 소속이 아닙니다.");
        }

        classroom.setTrainingState(TrainingState.RUNNING);
        classroom.setTrainingStartedAt(LocalDateTime.now());
        classroom.setTrainingEndedAt(null);
        classroom.setActiveScenario(scenario);
        classroom.setUpdatedAt(LocalDateTime.now());

        ClassroomV4 saved = classroomRepository.save(classroom);

        return TrainingControlResponse.builder()
                .classroomId(saved.getId())
                .trainingState(saved.getTrainingState() != null ? saved.getTrainingState().name() : null)
                .trainingStartedAt(saved.getTrainingStartedAt())
                .trainingEndedAt(saved.getTrainingEndedAt())
                .activeScenarioId(saved.getActiveScenario() != null ? saved.getActiveScenario().getId() : null)
                .build();
    }

    public TrainingControlResponse endTraining(String classroomId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        classroom.setTrainingState(TrainingState.ENDED);
        classroom.setTrainingEndedAt(LocalDateTime.now());
        classroom.setUpdatedAt(LocalDateTime.now());

        ClassroomV4 saved = classroomRepository.save(classroom);

        return TrainingControlResponse.builder()
                .classroomId(saved.getId())
                .trainingState(saved.getTrainingState() != null ? saved.getTrainingState().name() : null)
                .trainingStartedAt(saved.getTrainingStartedAt())
                .trainingEndedAt(saved.getTrainingEndedAt())
                .activeScenarioId(saved.getActiveScenario() != null ? saved.getActiveScenario().getId() : null)
                .build();
    }

    public List<StudentRoomResponse> getStudents(String classroomId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        return studentRepository.findByClassroom_IdOrderByJoinedAtAsc(classroom.getId())
                .stream()
                .map(student -> StudentRoomResponse.builder()
                        .studentId(student.getId())
                        .studentName(student.getStudentName())
                        .joinedAt(student.getJoinedAt())
                        .status(student.getStatus() != null ? student.getStatus().name() : null)
                        .isKicked(Boolean.TRUE.equals(student.getIsKicked()))
                        .build())
                .toList();
    }

    public StudentKickResponse kickStudent(String classroomId, String studentId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        StudentV4 student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        if (!student.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 학생은 이 교실 소속이 아닙니다.");
        }

        student.setIsKicked(true);
        studentRepository.save(student);

        return StudentKickResponse.builder()
                .studentId(student.getId())
                .classroomId(classroom.getId())
                .isKicked(student.getIsKicked())
                .message("학생이 퇴출 처리되었습니다.")
                .build();
    }

    public RoomMapVersionResponse createMapVersion(
            String classroomId,
            RoomMapVersionCreateRequest req
    ) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        SchoolV4 school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        UserV4 user = null;
        if (req.getCreatedBy() != null && !req.getCreatedBy().isBlank()) {
            user = userRepository.findById(req.getCreatedBy())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        }

        RoomMapVersionV4 mapVersion = RoomMapVersionV4.builder()
                .id(UUID.randomUUID().toString())
                .classroom(classroom)
                .school(school)
                .label(req.getLabel())
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .floorsJson(req.getFloorsJson())
                .build();

        roomMapVersionRepositoryV4.save(mapVersion);

        return RoomMapVersionResponse.builder()
                .mapVersionId(mapVersion.getId())
                .classroomId(classroom.getId())
                .label(mapVersion.getLabel())
                .createdAt(mapVersion.getCreatedAt())
                .build();
    }

    public ActiveMapResponse updateActiveMap(String classroomId, ActiveMapUpdateRequest req) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        RoomMapVersionV4 mapVersion = roomMapVersionRepositoryV4.findById(req.getMapVersionId())
                .orElseThrow(() -> new IllegalArgumentException("구조도 버전이 존재하지 않습니다."));

        if (mapVersion.getClassroom() == null || !mapVersion.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 구조도 버전은 이 교실 소속이 아닙니다.");
        }

        classroom.setActiveMapVersion(mapVersion);
        classroom.setUpdatedAt(LocalDateTime.now());

        ClassroomV4 saved = classroomRepository.save(classroom);

        return ActiveMapResponse.builder()
                .classroomId(saved.getId())
                .activeMapVersionId(
                        saved.getActiveMapVersion() != null
                                ? saved.getActiveMapVersion().getId()
                                : null
                )
                .label(
                        saved.getActiveMapVersion() != null
                                ? saved.getActiveMapVersion().getLabel()
                                : null
                )
                .build();
    }

    public GameStartContextResponse getGameStartContext(String classroomId) {

        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        ScenarioV4 scenario = classroom.getActiveScenario();
        RoomMapVersionV4 mapVersion = classroom.getActiveMapVersion();

        if (scenario == null) {
            throw new IllegalArgumentException("현재 활성 시나리오가 없습니다.");
        }

        if (mapVersion == null) {
            throw new IllegalArgumentException("현재 활성 구조도가 없습니다.");
        }

        return GameStartContextResponse.builder()
                .classroomId(classroom.getId())
                .scenarioId(scenario.getId())
                .scenarioType(scenario.getScenarioType() != null ? scenario.getScenarioType().name() : null)
                .trainingState(classroom.getTrainingState() != null ? classroom.getTrainingState().name() : null)
                .trainingStartedAt(classroom.getTrainingStartedAt())
                .activeMapVersionId(mapVersion.getId())
                .floorsJson(mapVersion.getFloorsJson())
                .npcPositionsJson(scenario.getNpcPositionsJson())
                .teamAssignmentJson(scenario.getTeamAssignmentJson())
                .build();
    }

    public java.util.List<RoomMapVersionSummaryResponse> getMapVersions(String classroomId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        String activeId = classroom.getActiveMapVersion() != null
                ? classroom.getActiveMapVersion().getId()
                : null;

        return roomMapVersionRepositoryV4
                .findByClassroom_IdOrderByCreatedAtDesc(classroom.getId())
                .stream()
                .map(v -> RoomMapVersionSummaryResponse.builder()
                        .mapVersionId(v.getId())
                        .label(v.getLabel())
                        .createdAt(v.getCreatedAt())
                        .createdByUserId(v.getCreatedBy() != null ? v.getCreatedBy().getId() : null)
                        .isActive(activeId != null && activeId.equals(v.getId()))
                        .build())
                .toList();
    }

    public RoomMapVersionDetailResponse getMapVersion(String classroomId, String mapVersionId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        RoomMapVersionV4 v = roomMapVersionRepositoryV4.findById(mapVersionId)
                .orElseThrow(() -> new IllegalArgumentException("구조도 버전이 존재하지 않습니다."));

        if (v.getClassroom() == null || !v.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 구조도는 이 교실 소속이 아닙니다.");
        }

        boolean isActive = classroom.getActiveMapVersion() != null
                && classroom.getActiveMapVersion().getId().equals(v.getId());

        return RoomMapVersionDetailResponse.builder()
                .mapVersionId(v.getId())
                .classroomId(classroom.getId())
                .schoolId(v.getSchool() != null ? v.getSchool().getId() : null)
                .label(v.getLabel())
                .floorsJson(v.getFloorsJson())
                .createdByUserId(v.getCreatedBy() != null ? v.getCreatedBy().getId() : null)
                .createdAt(v.getCreatedAt())
                .isActive(isActive)
                .build();
    }

    public RoomMapVersionDetailResponse updateMapVersion(
            String classroomId,
            String mapVersionId,
            RoomMapVersionUpdateRequest req
    ) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        RoomMapVersionV4 v = roomMapVersionRepositoryV4.findById(mapVersionId)
                .orElseThrow(() -> new IllegalArgumentException("구조도 버전이 존재하지 않습니다."));

        if (v.getClassroom() == null || !v.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 구조도는 이 교실 소속이 아닙니다.");
        }

        if (req.getLabel() != null) v.setLabel(req.getLabel());
        if (req.getFloorsJson() != null) v.setFloorsJson(req.getFloorsJson());

        RoomMapVersionV4 saved = roomMapVersionRepositoryV4.save(v);

        boolean isActive = classroom.getActiveMapVersion() != null
                && classroom.getActiveMapVersion().getId().equals(saved.getId());

        return RoomMapVersionDetailResponse.builder()
                .mapVersionId(saved.getId())
                .classroomId(classroom.getId())
                .schoolId(saved.getSchool() != null ? saved.getSchool().getId() : null)
                .label(saved.getLabel())
                .floorsJson(saved.getFloorsJson())
                .createdByUserId(saved.getCreatedBy() != null ? saved.getCreatedBy().getId() : null)
                .createdAt(saved.getCreatedAt())
                .isActive(isActive)
                .build();
    }
}