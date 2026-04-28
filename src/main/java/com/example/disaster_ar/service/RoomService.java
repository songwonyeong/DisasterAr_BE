package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.ChannelMapV4;
import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.RoomMapVersionV4;
import com.example.disaster_ar.domain.v4.ScenarioV4;
import com.example.disaster_ar.domain.v4.SchoolMapTemplateV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.domain.v4.UserV4;
import com.example.disaster_ar.domain.v4.enums.TrainingState;
import com.example.disaster_ar.dto.room.*;
import com.example.disaster_ar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.disaster_ar.domain.v4.BeaconV4;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.disaster_ar.domain.v4.ScenarioAssignmentV4;
import com.example.disaster_ar.domain.v4.ScenarioTriggerV4;
import com.example.disaster_ar.domain.v4.enums.TriggerReason;

import java.util.*;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final ClassroomRepository classroomRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final StudentRepositoryV4 studentRepository;
    private final ScenarioRepository scenarioRepository;
    private final RoomMapVersionRepositoryV4 roomMapVersionRepositoryV4;
    private final ChannelMapRepositoryV4 channelMapRepositoryV4;
    private final SchoolMapTemplateRepositoryV4 schoolMapTemplateRepositoryV4;
    private final ObjectMapper objectMapper;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final ScenarioTriggerRepositoryV4 scenarioTriggerRepositoryV4;
    private final ScenarioTeamMemberRepositoryV4 scenarioTeamMemberRepositoryV4;
    private final UserRepository userRepositoryV4;

    public RoomResponse createRoom(RoomCreateRequest req) {

        SchoolV4 school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        UserV4 owner = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        ClassroomV4 c = new ClassroomV4();
        if (c.getId() == null) c.setId(UUID.randomUUID().toString());

        c.setSchool(school);
        c.setOwner(owner);
        c.setClassName(req.getClassName());
        c.setStudentCount(0);
        c.setJoinCode(generateJoinCode());
        c.setActiveMapVersion(null);
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

        if (c.getOwner() == null || !c.getOwner().getId().equals(req.getUserId())) {
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

        if (c.getOwner() == null || !c.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("이 방을 삭제할 권한이 없습니다.");
        }

        classroomRepository.delete(c);
    }

    public RoomResponse regenerateJoinCode(String classroomId, String userId) {
        ClassroomV4 c = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (c.getOwner() == null || !c.getOwner().getId().equals(userId)) {
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

        createOnStartTriggers(saved, scenario);

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
                .map(student -> {

                    BeaconV4 beacon = student.getLastBeacon(); // ⭐ 반드시 여기 있어야 함

                    return StudentRoomResponse.builder()
                            .studentId(student.getId())
                            .studentName(student.getStudentName())
                            .joinedAt(student.getJoinedAt())
                            .status(student.getStatus() != null ? student.getStatus().name() : null)
                            .isKicked(Boolean.TRUE.equals(student.getIsKicked()))

                            .floorIndex(beacon != null ? beacon.getFloorIndex() : null)
                            .x(beacon != null ? beacon.getX() : null)
                            .y(beacon != null ? beacon.getY() : null)
                            .beaconId(beacon != null ? beacon.getId() : null)
                            .lastRssi(student.getLastBeaconRssi())
                            .lastSeenAt(
                                    student.getLastBeaconSeenAt() != null
                                            ? student.getLastBeaconSeenAt().toString()
                                            : null
                            )

                            .build();
                })
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

        validateNotRunning(classroom);

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

        validateNotRunning(classroom);

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

    public List<RoomMapVersionSummaryResponse> getMapVersions(String classroomId) {
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

        validateNotRunning(classroom);

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

    @Transactional
    public RoomMapVersionResponse createMapVersionFromChannel(
            String classroomId,
            FromChannelRequest req
    ) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        validateNotRunning(classroom);

        ChannelMapV4 channelMap = channelMapRepositoryV4.findById(req.getChannelMapId())
                .orElseThrow(() -> new IllegalArgumentException("채널 구조도가 존재하지 않습니다."));

        if (classroom.getSchool() == null || channelMap.getSchool() == null ||
                !classroom.getSchool().getId().equals(channelMap.getSchool().getId())) {
            throw new IllegalArgumentException("교실과 채널 구조도의 학교가 일치하지 않습니다.");
        }

        UserV4 user = null;
        if (req.getCreatedBy() != null && !req.getCreatedBy().isBlank()) {
            user = userRepository.findById(req.getCreatedBy())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        }

        RoomMapVersionV4 mapVersion = RoomMapVersionV4.builder()
                .id(UUID.randomUUID().toString())
                .classroom(classroom)
                .school(classroom.getSchool())
                .label(req.getLabel() != null && !req.getLabel().isBlank() ? req.getLabel() : "채널 구조도 기반 버전")
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .floorsJson(buildFloorsJsonFromChannelMap(channelMap))
                .build();

        roomMapVersionRepositoryV4.save(mapVersion);

        return RoomMapVersionResponse.builder()
                .mapVersionId(mapVersion.getId())
                .classroomId(classroom.getId())
                .label(mapVersion.getLabel())
                .createdAt(mapVersion.getCreatedAt())
                .build();
    }

    @Transactional
    public RoomMapVersionResponse createMapVersionFromTemplate(
            String classroomId,
            FromTemplateRequest req
    ) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        validateNotRunning(classroom);

        SchoolMapTemplateV4 template = schoolMapTemplateRepositoryV4.findById(req.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("템플릿이 존재하지 않습니다."));

        if (classroom.getSchool() == null || template.getSchool() == null ||
                !classroom.getSchool().getId().equals(template.getSchool().getId())) {
            throw new IllegalArgumentException("교실과 템플릿의 학교가 일치하지 않습니다.");
        }

        UserV4 user = null;
        if (req.getCreatedBy() != null && !req.getCreatedBy().isBlank()) {
            user = userRepository.findById(req.getCreatedBy())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        }

        RoomMapVersionV4 mapVersion = RoomMapVersionV4.builder()
                .id(UUID.randomUUID().toString())
                .classroom(classroom)
                .school(classroom.getSchool())
                .sourceTemplate(template)
                .label(req.getLabel() != null && !req.getLabel().isBlank() ? req.getLabel() : template.getTemplateName())
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .floorsJson(deepCopyJson(template.getFloorsJson()))
                .build();

        roomMapVersionRepositoryV4.save(mapVersion);

        return RoomMapVersionResponse.builder()
                .mapVersionId(mapVersion.getId())
                .classroomId(classroom.getId())
                .label(mapVersion.getLabel())
                .createdAt(mapVersion.getCreatedAt())
                .build();
    }

    @Transactional
    public String saveMapVersionAsTemplate(
            String classroomId,
            String mapVersionId,
            SaveTemplateRequest req
    ) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        RoomMapVersionV4 mapVersion = roomMapVersionRepositoryV4.findById(mapVersionId)
                .orElseThrow(() -> new IllegalArgumentException("구조도 버전이 존재하지 않습니다."));

        if (mapVersion.getClassroom() == null ||
                !mapVersion.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 구조도 버전은 이 교실 소속이 아닙니다.");
        }

        UserV4 user = null;
        if (req.getCreatedBy() != null && !req.getCreatedBy().isBlank()) {
            user = userRepository.findById(req.getCreatedBy())
                    .orElseThrow(() -> new IllegalArgumentException("사용자 없음"));
        }

        String templateName = req.getTemplateName();
        if (templateName == null || templateName.isBlank()) {
            throw new IllegalArgumentException("templateName은 필수입니다.");
        }

        SchoolMapTemplateV4 template = SchoolMapTemplateV4.builder()
                .id(UUID.randomUUID().toString())
                .school(classroom.getSchool())
                .templateName(templateName)
                .description(req.getDescription())
                .createdBy(user)
                .floorsJson(deepCopyJson(mapVersion.getFloorsJson()))
                .build();

        schoolMapTemplateRepositoryV4.save(template);
        return template.getId();
    }

    private void validateNotRunning(ClassroomV4 classroom) {
        if (classroom.getTrainingState() == TrainingState.RUNNING) {
            throw new IllegalStateException("훈련 중에는 구조도를 변경할 수 없습니다.");
        }
    }

    private String deepCopyJson(String json) {
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 처리 실패", e);
        }
    }

    private String buildFloorsJsonFromChannelMap(ChannelMapV4 channelMap) {
        try {
            Map<String, Object> floor = new LinkedHashMap<>();
            floor.put("floorIndex", channelMap.getFloorIndex());
            floor.put("floorLabel", channelMap.getFloorLabel());
            floor.put("imageUrl", channelMap.getUploadedImage());
            floor.put("scaleMPerPx", channelMap.getScaleMPerPx());
            floor.put("originX", channelMap.getOriginX());
            floor.put("originY", channelMap.getOriginY());

            Object elements = (channelMap.getElementsJson() == null || channelMap.getElementsJson().isBlank())
                    ? java.util.Collections.emptyList()
                    : objectMapper.readValue(channelMap.getElementsJson(), Object.class);

            floor.put("elements", elements);

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("floors", List.of(floor));

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalArgumentException("채널 구조도를 floorsJson으로 변환하는 데 실패했습니다.", e);
        }
    }

    @Transactional
    protected void createOnStartTriggers(ClassroomV4 classroom, ScenarioV4 scenario) {
        List<ScenarioAssignmentV4> assignments =
                scenarioAssignmentRepositoryV4.findByScenario_IdOrderByCreatedAtAsc(scenario.getId());

        if (assignments == null || assignments.isEmpty()) {
            return;
        }

        List<StudentV4> students = studentRepository.findByClassroom_IdOrderByJoinedAtAsc(classroom.getId());
        if (students == null || students.isEmpty()) {
            return;
        }

        for (ScenarioAssignmentV4 assignment : assignments) {
            if (!isOnStartAssignment(assignment)) {
                continue;
            }

            String targetType = assignment.getTargetType() != null
                    ? assignment.getTargetType().name()
                    : "ALL";

            if ("TEAM".equals(targetType)) {
                // 팀 대상은 일단 targetTeam 있는 경우에만 처리
                if (assignment.getTargetTeam() == null) {
                    continue;
                }

                for (StudentV4 student : students) {
                    boolean matched = scenarioTeamMemberMatchesStudent(
                            scenario.getId(),
                            assignment.getTargetTeam().getId(),
                            student.getId()
                    );

                    if (matched) {
                        createTriggerIfAbsent(scenario, assignment, student);
                    }
                }
            } else {
                // ALL, STUDENT, 기타는 우선 학생 전체 대상으로 처리
                for (StudentV4 student : students) {
                    createTriggerIfAbsent(scenario, assignment, student);
                }
            }
        }
    }

    private boolean isOnStartAssignment(ScenarioAssignmentV4 assignment) {
        if (assignment.getParamsJson() == null || assignment.getParamsJson().isBlank()) {
            return false;
        }

        try {
            Map<?, ?> map = objectMapper.readValue(assignment.getParamsJson(), Map.class);
            Object activationType = map.get("activationType");
            return activationType != null && "ON_START".equals(String.valueOf(activationType));
        } catch (Exception e) {
            return false;
        }
    }

    private void createTriggerIfAbsent(
            ScenarioV4 scenario,
            ScenarioAssignmentV4 assignment,
            StudentV4 student
    ) {
        boolean exists = scenarioTriggerRepositoryV4
                .findByScenario_IdAndStudent_IdAndAssignment_Id(
                        scenario.getId(),
                        student.getId(),
                        assignment.getId()
                )
                .isPresent();

        if (exists) {
            return;
        }

        ScenarioTriggerV4 trigger = ScenarioTriggerV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .assignment(assignment)
                .student(student)
                .triggerReason(TriggerReason.SYSTEM)
                .triggeredAt(LocalDateTime.now())
                .status("TRIGGERED")
                .payloadJson(buildOnStartPayload(assignment))
                .build();

        scenarioTriggerRepositoryV4.save(trigger);
    }

    private String buildOnStartPayload(ScenarioAssignmentV4 assignment) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of(
                            "activationType", "ON_START",
                            "assignmentId", assignment.getId()
                    )
            );
        } catch (Exception e) {
            return "{}";
        }
    }

    private boolean scenarioTeamMemberMatchesStudent(String scenarioId, String teamId, String studentId) {
        return scenarioTeamMemberRepositoryV4
                .findByScenario_IdAndTeam_IdAndStudent_Id(scenarioId, teamId, studentId)
                .isPresent();
    }

    public List<ActiveAssignmentResponse> getActiveAssignments(String classroomId, String studentId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        StudentV4 student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        if (student.getClassroom() == null || !student.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 학생은 이 교실 소속이 아닙니다.");
        }

        ScenarioV4 scenario = classroom.getActiveScenario();
        if (scenario == null) {
            return List.of();
        }

        return scenarioTriggerRepositoryV4
                .findByScenario_IdAndStudent_IdOrderByTriggeredAtDesc(scenario.getId(), student.getId())
                .stream()
                .map(trigger -> {
                    ScenarioAssignmentV4 assignment = trigger.getAssignment();

                    // content 연관관계가 있다면 사용
                    String contentId = null;
                    String title = null;
                    String description = null;

                    if (assignment.getContent() != null) {
                        contentId = assignment.getContent().getId();
                        title = assignment.getContent().getTitle();
                        description = assignment.getContent().getDescription();
                    }

                    return ActiveAssignmentResponse.builder()
                            .triggerId(trigger.getId())
                            .assignmentId(assignment != null ? assignment.getId() : null)
                            .assignmentType(
                                    assignment != null && assignment.getAssignmentType() != null
                                            ? assignment.getAssignmentType().name()
                                            : null
                            )
                            .contentId(contentId)
                            .title(title)
                            .description(description)
                            .floorIndex(assignment != null ? assignment.getFloorIndex() : null)
                            .beaconId(
                                    assignment != null && assignment.getBeacon() != null
                                            ? assignment.getBeacon().getId()
                                            : null
                            )
                            .triggeredAt(trigger.getTriggeredAt())
                            .build();
                })
                .toList();
    }

    @Transactional
    public RoomMapResponse createMapVersionFromChannelSet(
            String classroomId,
            CreateMapVersionFromChannelSetRequest req
    ) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        if (classroom.getSchool() == null) {
            throw new IllegalArgumentException("교실에 연결된 학교가 없습니다.");
        }

        if (!classroom.getSchool().getId().equals(req.getSchoolId())) {
            throw new IllegalArgumentException("요청 schoolId와 교실의 학교가 일치하지 않습니다.");
        }

        List<ChannelMapV4> channelMaps = channelMapRepositoryV4
                .findBySchool_IdOrderByFloorIndexAsc(req.getSchoolId());

        if (channelMaps.isEmpty()) {
            throw new IllegalArgumentException("해당 학교에 등록된 채널 구조도가 없습니다.");
        }

        Set<Integer> seen = new HashSet<>();
        for (ChannelMapV4 map : channelMaps) {
            if (map.getFloorIndex() == null) {
                throw new IllegalArgumentException("floorIndex가 비어 있는 채널 구조도가 있습니다. mapId=" + map.getId());
            }
            if (!seen.add(map.getFloorIndex())) {
                throw new IllegalArgumentException("중복된 floorIndex가 있습니다. floorIndex=" + map.getFloorIndex());
            }
        }

        String floorsJson = buildFloorsJsonFromChannelMaps(channelMaps);

        UserV4 createdBy = null;
        if (req.getCreatedByUserId() != null && !req.getCreatedByUserId().isBlank()) {
            createdBy = userRepositoryV4.findById(req.getCreatedByUserId())
                    .orElseThrow(() -> new IllegalArgumentException("createdBy 사용자가 존재하지 않습니다."));
        }

        RoomMapVersionV4 version = RoomMapVersionV4.builder()
                .id(UUID.randomUUID().toString())
                .classroom(classroom)
                .school(classroom.getSchool())
                .label(
                        req.getLabel() != null && !req.getLabel().isBlank()
                                ? req.getLabel()
                                : "채널 구조도 일괄 복사본"
                )
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .floorsJson(floorsJson)
                .build();

        roomMapVersionRepositoryV4.save(version);

        return RoomMapResponse.builder()
                .mapVersionId(version.getId())
                .classroomId(classroom.getId())
                .schoolId(classroom.getSchool().getId())   // ⭐ 추가
                .label(version.getLabel())
                .floorsJson(version.getFloorsJson())       // ⭐ 추가
                .createdAt(version.getCreatedAt())
                .build();
    }

    private String buildFloorsJsonFromChannelMaps(List<ChannelMapV4> channelMaps) {
        try {
            List<Map<String, Object>> floors = new ArrayList<>();

            for (ChannelMapV4 map : channelMaps) {
                Map<String, Object> floor = new LinkedHashMap<>();
                floor.put("sourceMapId", map.getId());
                floor.put("floorIndex", map.getFloorIndex());
                floor.put("floorLabel", map.getFloorLabel());
                floor.put("uploadedImage", map.getUploadedImage());
                floor.put("outlineJson", map.getOutlineJson());
                floor.put("scaleMPerPx", map.getScaleMPerPx());
                floor.put("originX", map.getOriginX());
                floor.put("originY", map.getOriginY());
                floor.put("elementsJson", parseJsonOrDefaultArray(map.getElementsJson()));

                floors.add(floor);
            }

            return objectMapper.writeValueAsString(floors);
        } catch (Exception e) {
            throw new IllegalArgumentException("채널 구조도 세트를 floorsJson으로 변환하는 중 오류가 발생했습니다.", e);
        }
    }

    private Object parseJsonOrDefaultArray(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Transactional
    public void updateActiveScenario(String classroomId, String scenarioId) {

        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        // ⭐ 같은 교실 소속인지 검증
        if (scenario.getClassroom() == null ||
                !scenario.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 시나리오는 이 교실 소속이 아닙니다.");
        }

        classroom.setActiveScenario(scenario);
        classroom.setUpdatedAt(LocalDateTime.now());

        classroomRepository.save(classroom);
    }
}