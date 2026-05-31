package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.ChannelMapV4;
import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.RoomMapVersionV4;
import com.example.disaster_ar.domain.v4.ScenarioV4;
import com.example.disaster_ar.domain.v4.SchoolMapTemplateV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.domain.v4.UserV4;
import com.example.disaster_ar.domain.v4.enums.*;
import com.example.disaster_ar.dto.room.*;
import com.example.disaster_ar.dto.scenario.TeamDistributionRequest;
import com.example.disaster_ar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.disaster_ar.domain.v4.BeaconV4;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.disaster_ar.domain.v4.ScenarioAssignmentV4;
import com.example.disaster_ar.domain.v4.ScenarioTriggerV4;
import com.example.disaster_ar.domain.v4.ScenarioTeamV4;
import com.example.disaster_ar.domain.v4.ScenarioTeamMemberV4;

import java.time.LocalDateTime;
import com.example.disaster_ar.domain.v4.ContentV4;

import java.util.Random;
import java.util.LinkedHashMap;
import com.example.disaster_ar.domain.v4.ItemV4;
import com.example.disaster_ar.domain.v4.StudentItemV4;
import com.example.disaster_ar.domain.v4.StudentMissionProgressV4;
import com.example.disaster_ar.domain.v4.ScenarioActionEventV4;
import com.example.disaster_ar.domain.v4.TeamMissionProgressV4;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityManager;

import java.util.*;

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
    private final ScenarioAssignmentService scenarioAssignmentService;
    private final ScenarioTeamRepositoryV4 scenarioTeamRepositoryV4;
    private final StudentRepositoryV4 studentRepositoryV4;
    private final ContentRepository contentRepository;
    private final StudentMissionProgressRepositoryV4 studentMissionProgressRepository;
    private final ItemRepositoryV4 itemRepositoryV4;
    private final StudentItemRepositoryV4 studentItemRepositoryV4;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;
    private final TeamMissionProgressRepositoryV4 teamMissionProgressRepositoryV4;
    private final BeaconAutoMappingService beaconAutoMappingService;
    private final EntityManager entityManager;
    private final ScenarioTeamDistributionService scenarioTeamDistributionService;
    private final ScenarioTeamAssignmentService scenarioTeamAssignmentService;


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

    @Transactional
    public void deleteRoom(String classroomId, String userId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (classroom.getOwner() == null || !classroom.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("이 방을 삭제할 권한이 없습니다.");
        }

        if (classroom.getTrainingState() == TrainingState.RUNNING) {
            throw new IllegalStateException("훈련 중인 방은 삭제할 수 없습니다. 훈련 종료 후 삭제해주세요.");
        }

        deleteClassroomGraph(classroom);
    }

    private void deleteClassroomGraph(ClassroomV4 classroom) {
        String classroomId = classroom.getId();

        /*
         * classrooms가 scenarios, room_map_versions를 active FK로 물고 있으므로
         * 먼저 참조를 끊는다.
         */
        classroom.setActiveScenario(null);
        classroom.setActiveMapVersion(null);
        classroom.setTrainingState(TrainingState.WAITING);
        classroom.setTrainingStartedAt(null);
        classroom.setTrainingEndedAt(null);
        classroom.setUpdatedAt(LocalDateTime.now());

        classroomRepository.saveAndFlush(classroom);

        /*
         * 이후 native delete를 사용할 것이므로,
         * JPA 영속성 컨텍스트에 남아있는 classroom 상태를 비운다.
         */
        entityManager.clear();

        /*
         * 삭제 순서 중요.
         * FK를 물고 있는 하위 테이블부터 지우고,
         * 마지막에 students, room_map_versions, classrooms를 지운다.
         */

        executeDelete("""
        delete from evaluations
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
        or team_id in (
            select st.id
            from scenario_teams st
            join scenarios s on st.scenario_id = s.id
            where s.classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from scenario_action_events
        where classroom_id = :classroomId
        or scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from chat_messages
        where classroom_id = :classroomId
        or scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from student_beacon_events
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from quiz_submissions
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from card_quiz_submissions
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from student_items
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from scenario_triggers
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from student_mission_progress
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from team_mission_step_progress
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or team_id in (
            select st.id
            from scenario_teams st
            join scenarios s on st.scenario_id = s.id
            where s.classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from team_mission_progress
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or team_id in (
            select st.id
            from scenario_teams st
            join scenarios s on st.scenario_id = s.id
            where s.classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from scenario_team_members
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
        or student_id in (
            select id from students where classroom_id = :classroomId
        )
        or team_id in (
            select st.id
            from scenario_teams st
            join scenarios s on st.scenario_id = s.id
            where s.classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from scenario_npcs
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
    """, classroomId);

        /*
         * scenario_assignments는 target_team_id로 scenario_teams를 참조할 수 있으므로
         * scenario_teams보다 먼저 삭제한다.
         */
        executeDelete("""
        delete from scenario_assignments
        where classroom_id = :classroomId
        or scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
    """, classroomId);

        executeDelete("""
        delete from scenario_teams
        where scenario_id in (
            select id from scenarios where classroom_id = :classroomId
        )
    """, classroomId);

        /*
         * 이제 scenario 하위 데이터가 지워졌으므로 scenarios 삭제.
         */
        executeDelete("""
        delete from scenarios
        where classroom_id = :classroomId
    """, classroomId);

        /*
         * students는 여러 테이블에서 참조하므로 거의 마지막에 삭제.
         */
        executeDelete("""
        delete from students
        where classroom_id = :classroomId
    """, classroomId);

        /*
         * room_map_versions는 classroom.active_map_version_id와 scenarios.map_version_id 참조를 끊은 뒤 삭제.
         */
        executeDelete("""
        delete from room_map_versions
        where classroom_id = :classroomId
    """, classroomId);

        /*
         * 마지막으로 classroom 삭제.
         */
        executeDelete("""
        delete from classrooms
        where id = :classroomId
    """, classroomId);
    }

    private int executeDelete(String sql, String classroomId) {
        return entityManager.createNativeQuery(sql)
                .setParameter("classroomId", classroomId)
                .executeUpdate();
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

    @Transactional
    public TrainingControlResponse startTraining(String classroomId, TrainingStartRequest req) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        ScenarioV4 scenario = scenarioRepository.findById(req.getScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (scenario.getClassroom() == null || !scenario.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 시나리오는 이 교실 소속이 아닙니다.");
        }

        /*
         * 2차:
         * 훈련 시작 직전에 현재 활성 구조도 기준으로 비콘-구역 매핑을 최신화한다.
         * RUNNING 전환 전에 실행해야 한다.
         */
        beaconAutoMappingService.syncForActiveMap(classroom);

        applyDisasterOriginFromActiveMapIfNeeded(classroom, scenario);
        applyRandomScenarioEventIfNeeded(scenario);
        scenarioRepository.save(scenario);

        classroom.setTrainingState(TrainingState.RUNNING);

        classroom.setTrainingStartedAt(LocalDateTime.now());
        classroom.setTrainingEndedAt(null);
        classroom.setActiveScenario(scenario);
        classroom.setUpdatedAt(LocalDateTime.now());

        ClassroomV4 saved = classroomRepository.save(classroom);

        scenarioAssignmentService.createDefaultFireAssignmentsIfEmpty(scenario.getId(), saved.getId());

        prepareTeamsForTraining(scenario, saved.getId());

        linkFireTeamAssignments(scenario.getId());

        createOnStartTriggers(saved, scenario);

        return TrainingControlResponse.builder()
                .classroomId(saved.getId())
                .trainingState(saved.getTrainingState() != null ? saved.getTrainingState().name() : null)
                .trainingStartedAt(saved.getTrainingStartedAt())
                .trainingEndedAt(saved.getTrainingEndedAt())
                .activeScenarioId(saved.getActiveScenario() != null ? saved.getActiveScenario().getId() : null)
                .build();
    }

    private ScenarioTeamV4 findOrCreateTeam(
            ScenarioV4 scenario,
            String teamCode,
            String teamName
    ) {
        return scenarioTeamRepositoryV4
                .findByScenario_IdAndTeamCode(scenario.getId(), teamCode)
                .orElseGet(() -> {
                    ScenarioTeamV4 team = ScenarioTeamV4.builder()
                            .id(UUID.randomUUID().toString())
                            .scenario(scenario)
                            .teamCode(teamCode)
                            .teamName(teamName)
                            .build();

                    return scenarioTeamRepositoryV4.save(team);
                });
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

    @Transactional
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

        /*
         * 2차:
         * 활성 구조도 기준으로 비콘이 어느 zone 안에 있는지 자동 판정하고
         * beacon_element_maps를 자동 갱신한다.
         */
        BeaconAutoMappingService.SyncResult syncResult =
                beaconAutoMappingService.syncForActiveMap(saved);

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
                .syncFloorsProcessed(syncResult.floorsProcessed())
                .syncBeaconsProcessed(syncResult.beaconsProcessed())
                .syncMappingsCreated(syncResult.mappingsCreated())
                .syncMappingsUpdated(syncResult.mappingsUpdated())
                .syncMappingsDeactivated(syncResult.mappingsDeactivated())
                .syncUnmatchedBeacons(syncResult.unmatchedBeacons())
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
                .trainingTimeSeconds(scenario != null ? scenario.getTrainTime() : null)
                .activeMapVersionId(mapVersion.getId())
                .floorsJson(mapVersion.getFloorsJson())

                .disasterOriginFloorIndex(scenario.getDisasterOriginFloorIndex())
                .disasterOriginElementId(scenario.getDisasterOriginElementId())
                .disasterOriginName(scenario.getDisasterOriginName())
                .disasterMessage(buildDisasterMessage(scenario))
                .warningMessage(buildWarningMessage(classroom, scenario))

                .scenarioEventContentId(
                        scenario.getSelectedScenarioEventContent() != null
                                ? scenario.getSelectedScenarioEventContent().getId()
                                : null
                )
                .scenarioEventPlace(
                        scenario.getSelectedScenarioEventContent() != null
                                ? scenario.getSelectedScenarioEventContent().getPlace()
                                : null
                )
                .scenarioEventReason(
                        scenario.getSelectedScenarioEventContent() != null
                                ? scenario.getSelectedScenarioEventContent().getReason()
                                : null
                )

                .npcPositionsJson(scenario.getNpcPositionsJson())
                .teamAssignmentJson(scenario.getTeamAssignmentJson())
                .build();
    }

    private void applyRandomScenarioEventIfNeeded(ScenarioV4 scenario) {
        if (scenario.getScenarioType() == null || !"FIRE".equals(scenario.getScenarioType().name())) {
            return;
        }

        // 이미 선택된 이벤트가 있으면 다시 뽑지 않음
        if (scenario.getSelectedScenarioEventContent() != null) {
            return;
        }

        String originName = scenario.getDisasterOriginName();

        List<ContentV4> candidates = findScenarioEventCandidates(originName);

        if (candidates.isEmpty()) {
            return;
        }

        ContentV4 selected = candidates.get(new Random().nextInt(candidates.size()));

        scenario.setSelectedScenarioEventContent(selected);

        Map<String, Object> aiDecision = new LinkedHashMap<>();
        aiDecision.put("source", "SPRING_RANDOM_SCENARIO_EVENT");
        aiDecision.put("matchedOriginName", originName);
        aiDecision.put("selectedContentId", selected.getId());
        aiDecision.put("place", selected.getPlace());
        aiDecision.put("reason", selected.getReason());

        try {
            scenario.setAiDecisionJson(objectMapper.writeValueAsString(aiDecision));
        } catch (Exception ignored) {
        }
    }

    private List<ContentV4> findScenarioEventCandidates(String originName) {
        List<ContentV4> allEvents = contentRepository.findByContentType(ContentType.SCENARIO_EVENT);

        if (allEvents == null || allEvents.isEmpty()) {
            return List.of();
        }

        if (originName == null || originName.isBlank()) {
            return allEvents;
        }

        String normalizedOrigin = normalizePlace(originName);

        List<ContentV4> matched = allEvents.stream()
                .filter(content -> {
                    String place = normalizePlace(content.getPlace());

                    if (place == null || place.isBlank()) {
                        return false;
                    }

                    return normalizedOrigin.equals(place)
                            || normalizedOrigin.contains(place)
                            || place.contains(normalizedOrigin);
                })
                .toList();

        // 장소 매칭 실패 시 전체 화재 이벤트 중 랜덤 fallback
        return matched.isEmpty() ? allEvents : matched;
    }

    private String normalizePlace(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace(" ", "")
                .replace("1층", "")
                .replace("2층", "")
                .replace("3층", "")
                .replace("4층", "")
                .replace("5층", "")
                .replace("층", "")
                .trim();
    }

    private void applyDisasterOriginFromActiveMapIfNeeded(
            ClassroomV4 classroom,
            ScenarioV4 scenario
    ) {
        if (scenario.getScenarioType() == null || !"FIRE".equals(scenario.getScenarioType().name())) {
            return;
        }

        // 이미 화재 위치가 있으면 다시 덮어쓰지 않음
        if (scenario.getDisasterOriginElementId() != null && !scenario.getDisasterOriginElementId().isBlank()) {
            return;
        }

        RoomMapVersionV4 mapVersion = classroom.getActiveMapVersion();
        if (mapVersion == null || mapVersion.getFloorsJson() == null || mapVersion.getFloorsJson().isBlank()) {
            return;
        }

        FireOriginCandidate candidate = findFireOriginCandidate(mapVersion.getFloorsJson());

        if (candidate == null) {
            return;
        }

        scenario.setDisasterOriginFloorIndex(candidate.floorIndex);
        scenario.setDisasterOriginElementId(candidate.elementId);
        scenario.setDisasterOriginName(candidate.name);
        scenario.setLocation(candidate.floorLabel != null && !candidate.floorLabel.isBlank()
                ? candidate.floorLabel + " " + candidate.name
                : candidate.name
        );
    }

    private FireOriginCandidate findFireOriginCandidate(String floorsJson) {
        try {
            Object root = objectMapper.readValue(floorsJson, Object.class);

            List<?> floors;

            if (root instanceof Map<?, ?> rootMap && rootMap.get("floors") instanceof List<?> list) {
                floors = list;
            } else if (root instanceof List<?> list) {
                floors = list;
            } else {
                return null;
            }

            List<FireOriginCandidate> candidates = new ArrayList<>();

            for (Object floorObj : floors) {
                if (!(floorObj instanceof Map<?, ?> floor)) {
                    continue;
                }

                Integer floorIndex = asInteger(firstNonNull(
                        floor.get("floorIndex"),
                        floor.get("floor_index")
                ));

                String floorLabel = asString(firstNonNull(
                        floor.get("floorLabel"),
                        floor.get("floor_label")
                ));

                Object elementsObj = firstNonNull(
                        floor.get("elements"),
                        floor.get("elementsJson"),
                        floor.get("elements_json")
                );

                if (elementsObj instanceof String elementsString) {
                    try {
                        elementsObj = objectMapper.readValue(elementsString, Object.class);
                    } catch (Exception ignored) {
                        continue;
                    }
                }

                if (!(elementsObj instanceof List<?> elements)) {
                    continue;
                }

                for (Object elementObj : elements) {
                    if (!(elementObj instanceof Map<?, ?> element)) {
                        continue;
                    }

                    if (!isFireElement(element)) {
                        continue;
                    }

                    String elementId = asString(firstNonNull(
                            element.get("elementId"),
                            element.get("element_id"),
                            element.get("id")
                    ));

                    String name = asString(firstNonNull(
                            element.get("name"),
                            element.get("label"),
                            element.get("elementName"),
                            element.get("element_name")
                    ));

                    if (elementId == null || elementId.isBlank()) {
                        elementId = name;
                    }

                    if (name == null || name.isBlank()) {
                        name = elementId;
                    }

                    if (name == null || name.isBlank()) {
                        name = "화재구역";
                    }

                    candidates.add(new FireOriginCandidate(
                            floorIndex != null ? floorIndex : 0,
                            floorLabel,
                            elementId,
                            name
                    ));
                }
            }

            if (candidates.isEmpty()) {
                System.out.println("🔥 화재구역 후보 없음");
                return null;
            }

            System.out.println("🔥 화재구역 후보 개수 = " + candidates.size());

            for (FireOriginCandidate c : candidates) {
                System.out.println("🔥 후보 = " + c.elementId() + " / " + c.name());
            }

            FireOriginCandidate selected = candidates.get(new Random().nextInt(candidates.size()));

            System.out.println("🔥 선택된 화재구역 = " + selected.elementId() + " / " + selected.name());

            return selected;

        } catch (Exception e) {
            return null;
        }
    }

    private boolean isFireElement(Map<?, ?> element) {
        String elementType = asString(firstNonNull(
                element.get("elementType"),
                element.get("element_type"),
                element.get("type")
        ));

        String name = asString(firstNonNull(
                element.get("name"),
                element.get("label"),
                element.get("elementName"),
                element.get("element_name")
        ));

        Object tagsObj = firstNonNull(
                element.get("tags"),
                element.get("tag"),
                element.get("tagsJson"),
                element.get("tags_json")
        );

        String merged = "";

        if (elementType != null) {
            merged += " " + elementType;
        }
        if (name != null) {
            merged += " " + name;
        }
        if (tagsObj != null) {
            merged += " " + tagsObj;
        }

        String upper = merged.toUpperCase();

        return upper.contains("FIRE")
                || upper.contains("DISASTER_ZONE")
                || upper.contains("FIRE_ZONE")
                || merged.contains("화재")
                || merged.contains("위험구역")
                || merged.contains("재난");
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String buildDisasterMessage(ScenarioV4 scenario) {
        if (scenario.getScenarioType() == null || !"FIRE".equals(scenario.getScenarioType().name())) {
            return null;
        }

        String locationText = null;

        if (scenario.getLocation() != null && !scenario.getLocation().isBlank()) {
            locationText = scenario.getLocation();
        } else if (scenario.getDisasterOriginName() != null && !scenario.getDisasterOriginName().isBlank()) {
            locationText = scenario.getDisasterOriginName();
        }

        if (locationText == null || locationText.isBlank()) {
            return "화재가 발생했습니다.";
        }

        ContentV4 event = scenario.getSelectedScenarioEventContent();

        if (event != null && event.getReason() != null && !event.getReason().isBlank()) {
            return locationText + "에서 " + event.getReason();
        }

        return locationText + "에서 화재가 발생했습니다.";
    }

    private record FireOriginCandidate(
            Integer floorIndex,
            String floorLabel,
            String elementId,
            String name
    ) {
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

    @Transactional
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

        if (isActive) {
            beaconAutoMappingService.syncForActiveMap(classroom);
        }

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

        // active-assignments 조회 시점 보정
        scenarioAssignmentService.createDefaultFireAssignmentsIfEmpty(
                scenario.getId(),
                classroom.getId()
        );

        assignTeamsIfEmpty(
                scenario.getId(),
                classroom.getId()
        );

        linkFireTeamAssignments(
                scenario.getId()
        );

        createOnStartTriggers(
                classroom,
                scenario
        );

        return scenarioTriggerRepositoryV4
                .findByScenario_IdAndStudent_IdOrderByTriggeredAtDesc(scenario.getId(), student.getId())
                .stream()
                .map(trigger -> {
                    ScenarioAssignmentV4 assignment = trigger.getAssignment();

                    String contentId = null;
                    String title = null;
                    String description = null;

                    if (assignment.getContent() != null) {
                        contentId = assignment.getContent().getId();
                        title = assignment.getContent().getTitle();
                        description = assignment.getContent().getDescription();
                    }

                    MissionProgressView progressView = resolveMissionProgress(
                            scenario,
                            assignment,
                            student,
                            trigger
                    );

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

                            .targetType(
                                    assignment != null && assignment.getTargetType() != null
                                            ? assignment.getTargetType().name()
                                            : null
                            )
                            .targetTeamId(
                                    assignment != null && assignment.getTargetTeam() != null
                                            ? assignment.getTargetTeam().getId()
                                            : null
                            )
                            .paramsJson(
                                    assignment != null ? assignment.getParamsJson() : null
                            )
                            .missionCode(
                                    assignment != null ? extractMissionCode(assignment.getParamsJson(), title) : null
                            )

                            .requiredCount(progressView.requiredCount())
                            .progressCount(progressView.progressCount())
                            .status(progressView.status())

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

    private MissionProgressView resolveMissionProgress(
            ScenarioV4 scenario,
            ScenarioAssignmentV4 assignment,
            StudentV4 student,
            ScenarioTriggerV4 trigger
    ) {
        if (scenario == null || assignment == null || student == null) {
            return new MissionProgressView(
                    1,
                    0,
                    normalizeTriggerStatus(trigger != null ? trigger.getStatus() : null)
            );
        }

        String missionCode = extractMissionCode(
                assignment.getParamsJson(),
                assignment.getContent() != null ? assignment.getContent().getTitle() : null
        );

        // 도넛 게임은 팀 progress 기준 + 팀 퀴즈 대기 상태까지 반영
        if ("FIRETEAM_PUT_OUT_FIRE".equals(missionCode)) {
            return scenarioTeamMemberRepositoryV4
                    .findByScenario_IdAndStudent_Id(
                            scenario.getId(),
                            student.getId()
                    )
                    .flatMap(member -> {
                        if (member.getTeam() == null) {
                            return Optional.empty();
                        }

                        return teamMissionProgressRepositoryV4
                                .findByScenario_IdAndAssignment_IdAndTeam_Id(
                                        scenario.getId(),
                                        assignment.getId(),
                                        member.getTeam().getId()
                                );
                    })
                    .map(progress -> new MissionProgressView(
                            progress.getRequiredCount() != null
                                    ? progress.getRequiredCount()
                                    : resolveDefaultRequiredCount(assignment),
                            progress.getProgressCount() != null
                                    ? progress.getProgressCount()
                                    : 0,
                            resolveFireteamPutOutFireStatus(
                                    scenario,
                                    assignment,
                                    student,
                                    trigger,
                                    progress
                            )
                    ))
                    .orElseGet(() -> new MissionProgressView(
                            resolveDefaultRequiredCount(assignment),
                            0,
                            resolveFireteamPutOutFireStatus(
                                    scenario,
                                    assignment,
                                    student,
                                    trigger,
                                    null
                            )
                    ));
        }

        // 나머지는 학생 개인 progress 기준
        return studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenario.getId(),
                        assignment.getId(),
                        student.getId()
                )
                .map(progress -> new MissionProgressView(
                        progress.getRequiredCount() != null
                                ? progress.getRequiredCount()
                                : resolveDefaultRequiredCount(assignment),
                        progress.getProgressCount() != null
                                ? progress.getProgressCount()
                                : 0,
                        progress.getStatus() != null
                                ? progress.getStatus().name()
                                : normalizeTriggerStatus(trigger.getStatus())
                ))
                .orElseGet(() -> new MissionProgressView(
                        resolveDefaultRequiredCount(assignment),
                        0,
                        normalizeTriggerStatus(trigger.getStatus())
                ));
    }

    private String resolveFireteamPutOutFireStatus(
            ScenarioV4 scenario,
            ScenarioAssignmentV4 donutAssignment,
            StudentV4 student,
            ScenarioTriggerV4 trigger,
            TeamMissionProgressV4 donutProgress
    ) {
        // 도넛 게임 자체가 이미 완료됐으면 무조건 COMPLETED
        if (donutProgress != null && donutProgress.getStatus() == ProgressStatus.COMPLETED) {
            return "COMPLETED";
        }

        // 소화기 사용 퀴즈 assignment 찾기
        ScenarioAssignmentV4 quizAssignment = scenarioAssignmentRepositoryV4
                .findByScenario_IdOrderByCreatedAtAsc(scenario.getId())
                .stream()
                .filter(assignment -> "FIRETEAM_EXTINGUISHER_QUIZ".equals(
                        extractMissionCode(
                                assignment.getParamsJson(),
                                assignment.getContent() != null
                                        ? assignment.getContent().getTitle()
                                        : null
                        )
                ))
                .findFirst()
                .orElse(null);

        if (quizAssignment == null) {
            return normalizeTriggerStatus(
                    trigger != null ? trigger.getStatus() : null
            );
        }

        // 현재 학생의 팀 조회
        Optional<ScenarioTeamMemberV4> memberOpt =
                scenarioTeamMemberRepositoryV4.findByScenario_IdAndStudent_Id(
                        scenario.getId(),
                        student.getId()
                );

        if (memberOpt.isEmpty() || memberOpt.get().getTeam() == null) {
            return normalizeTriggerStatus(
                    trigger != null ? trigger.getStatus() : null
            );
        }

        ScenarioTeamV4 team = memberOpt.get().getTeam();

        // 같은 FIRE 팀원 전체 조회
        List<ScenarioTeamMemberV4> members =
                scenarioTeamMemberRepositoryV4.findByScenario_IdAndTeam_IdOrderByAssignedAtAsc(
                        scenario.getId(),
                        team.getId()
                );

        int total = members.size();
        int completed = 0;
        LocalDateTime allCompletedAt = null;

        for (ScenarioTeamMemberV4 member : members) {
            if (member.getStudent() == null) {
                continue;
            }

            StudentMissionProgressV4 quizProgress = studentMissionProgressRepository
                    .findByScenario_IdAndAssignment_IdAndStudent_Id(
                            scenario.getId(),
                            quizAssignment.getId(),
                            member.getStudent().getId()
                    )
                    .orElse(null);

            if (quizProgress != null && quizProgress.getStatus() == ProgressStatus.COMPLETED) {
                completed++;

                LocalDateTime completedAt = quizProgress.getCompletedAt() != null
                        ? quizProgress.getCompletedAt()
                        : LocalDateTime.now();

                if (allCompletedAt == null || completedAt.isAfter(allCompletedAt)) {
                    allCompletedAt = completedAt;
                }
            }
        }

        // 팀원 중 아직 소화기 퀴즈 미완료자가 있으면 대기
        if (total == 0 || completed < total) {
            return "WAITING_TEAM";
        }

        // 팀원 전원 퀴즈 완료 후 5초 대기
        if (allCompletedAt != null) {
            long elapsedSeconds = Duration
                    .between(allCompletedAt, LocalDateTime.now())
                    .getSeconds();

            if (elapsedSeconds < 5) {
                return "WAITING_DELAY";
            }
        }

        // 전원 퀴즈 완료 + 5초 대기 완료
        return "IN_PROGRESS";
    }

    private Integer resolveDefaultRequiredCount(ScenarioAssignmentV4 assignment) {
        if (assignment == null) {
            return 1;
        }

        String missionCode = extractMissionCode(
                assignment.getParamsJson(),
                assignment.getContent() != null ? assignment.getContent().getTitle() : null
        );

        if ("COMMON_RANDOM_QUIZ".equals(missionCode)) {
            return getIntParam(assignment.getParamsJson(), "requiredCorrectCount", 3);
        }

        if ("FIRETEAM_PUT_OUT_FIRE".equals(missionCode)) {
            return getIntParam(assignment.getParamsJson(), "requiredClickCount", 30);
        }

        return getIntParam(assignment.getParamsJson(), "requiredCount", 1);
    }

    private String normalizeTriggerStatus(String triggerStatus) {
        if (triggerStatus == null || triggerStatus.isBlank()) {
            return "IN_PROGRESS";
        }

        if ("TRIGGERED".equalsIgnoreCase(triggerStatus)) {
            return "IN_PROGRESS";
        }

        return triggerStatus;
    }

    private int getIntParam(String paramsJson, String key, int defaultValue) {
        if (paramsJson == null || paramsJson.isBlank()) {
            return defaultValue;
        }

        try {
            Map<?, ?> map = objectMapper.readValue(paramsJson, Map.class);

            Object value = map.get(key);

            if (value instanceof Number number) {
                return number.intValue();
            }

            if (value != null) {
                return Integer.parseInt(String.valueOf(value));
            }

            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private record MissionProgressView(
            Integer requiredCount,
            Integer progressCount,
            String status
    ) {
    }

    private String extractMissionCode(String paramsJson, String title) {
        if (paramsJson != null && !paramsJson.isBlank()) {
            try {
                Map<?, ?> map = objectMapper.readValue(paramsJson, Map.class);
                Object missionCode = map.get("missionCode");
                if (missionCode != null) {
                    return String.valueOf(missionCode);
                }
            } catch (Exception ignored) {
            }
        }

        if ("랜덤 퀴즈 3개 이상 맞추기".equals(title)) {
            return "COMMON_RANDOM_QUIZ";
        }
        if ("119 신고 순서 맞추기".equals(title)) {
            return "COMMON_REPORT_CALL";
        }
        if ("소화기 찾기".equals(title)) {
            return "COMMON_FIND_EXTINGUISHER";
        }
        if ("제한 시간 내 안전구역 도착".equals(title)) {
            return "COMMON_SAFE_ZONE";
        }
        if ("소화팀: 소화기 획득".equals(title)) {
            return "FIRETEAM_GET_EXTINGUISHER";
        }
        if ("소화팀: 소화기 사용 퀴즈".equals(title)) {
            return "FIRETEAM_EXTINGUISHER_QUIZ";
        }
        if ("소화팀: 도넛 게임으로 불 끄기".equals(title)) {
            return "FIRETEAM_PUT_OUT_FIRE";
        }

        return null;
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

    @Transactional
    public void assignTeamsIfEmpty(String scenarioId, String classroomId) {

        // 이미 학생 팀 배정이 있으면 패스
        if (scenarioTeamMemberRepositoryV4.existsByScenario_Id(scenarioId)) {
            return;
        }

        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        List<StudentV4> students = new ArrayList<>(
                studentRepositoryV4.findByClassroom_IdOrderByJoinedAtAsc(classroomId)
                        .stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getIsKicked()))
                        .toList()
        );

        if (students.isEmpty()) {
            System.out.println("⚠ 학생 없음 → 팀 배정 스킵");
            return;
        }

        List<ScenarioTeamV4> teams = scenarioTeamRepositoryV4
                .findByScenario_IdOrderByTeamCodeAsc(scenarioId);

        // team-distribution API를 안 불렀다면 기존처럼 AUTO로 생성
        if (teams == null || teams.isEmpty()) {
            int total = students.size();

            int civilianCount = (total * 5) / 10;
            int fireCount = (total * 3) / 10;
            int emergencyCount = total - civilianCount - fireCount;

            ScenarioTeamV4 civilianTeam = findOrCreateTeamWithMaxMembers(
                    scenario, "CIVILIAN", "시민", civilianCount
            );
            ScenarioTeamV4 fireTeam = findOrCreateTeamWithMaxMembers(
                    scenario, "FIRE", "소화팀", fireCount
            );
            ScenarioTeamV4 emergencyTeam = findOrCreateTeamWithMaxMembers(
                    scenario, "EMERGENCY", "응급팀", emergencyCount
            );

            teams = List.of(civilianTeam, fireTeam, emergencyTeam);
        }

        Collections.shuffle(students);

        int totalMax = teams.stream()
                .mapToInt(t -> t.getMaxMembers() != null ? t.getMaxMembers() : 0)
                .sum();

        if (totalMax != students.size()) {
            throw new IllegalArgumentException(
                    "팀별 maxMembers 합계와 학생 수가 일치하지 않습니다. maxMembers="
                            + totalMax + ", students=" + students.size()
            );
        }

        List<ScenarioTeamMemberV4> members = new ArrayList<>();

        int index = 0;

        for (ScenarioTeamV4 team : teams) {
            int count = team.getMaxMembers() != null ? team.getMaxMembers() : 0;

            for (int i = 0; i < count && index < students.size(); i++) {
                members.add(createMember(scenario, students.get(index++), team));
            }
        }

        scenarioTeamMemberRepositoryV4.saveAll(members);

        System.out.println("🔥 팀 배정 완료: " + members.size() + "명");
    }

    private void prepareTeamsForTraining(ScenarioV4 scenario, String classroomId) {
        List<StudentV4> activeStudents =
                studentRepositoryV4.findByClassroom_IdAndIsKickedFalseOrderByJoinedAtAsc(classroomId);

        if (activeStudents == null || activeStudents.isEmpty()) {
            System.out.println("⚠ 활성 학생 없음 → 팀 배정 스킵");
            return;
        }

        TeamMode teamMode = scenario.getTeamMode();

        /*
         * AUTO 모드에서는 기존 scenario_team_members가 있더라도
         * 현재 활성 학생 기준으로 팀 정원 재계산 + 재배정을 강제한다.
         *
         * 이유:
         * - 강퇴/재입장 학생 때문에 기존 team_members가 남아 있을 수 있음
         * - 기존 배정이 있으면 assignTeamsIfEmpty()가 스킵되어
         *   4명이 전부 시민팀처럼 보이는 문제가 생김
         */
        if (teamMode == null || teamMode == TeamMode.AUTO) {
            scenarioTeamDistributionService.distributeTeams(
                    scenario.getId(),
                    TeamDistributionRequest.builder()
                            .mode("AUTO")
                            .build()
            );

            scenarioTeamAssignmentService.assignStudents(scenario.getId());
            return;
        }

        /*
         * MANUAL 모드는 기존 배정을 유지한다.
         * 단, 아직 배정이 전혀 없으면 기존 fallback 로직으로 한 번 배정한다.
         */
        if (!scenarioTeamMemberRepositoryV4.existsByScenario_Id(scenario.getId())) {
            assignTeamsIfEmpty(scenario.getId(), classroomId);
        }
    }

    private ScenarioTeamV4 findOrCreateTeamWithMaxMembers(
            ScenarioV4 scenario,
            String teamCode,
            String teamName,
            Integer maxMembers
    ) {
        ScenarioTeamV4 team = scenarioTeamRepositoryV4
                .findByScenario_IdAndTeamCode(scenario.getId(), teamCode)
                .orElseGet(() -> ScenarioTeamV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .teamCode(teamCode)
                        .teamName(teamName)
                        .build()
                );

        team.setTeamName(teamName);
        team.setMaxMembers(maxMembers);
        team.setMinMembers(0);

        return scenarioTeamRepositoryV4.save(team);
    }

    private ScenarioTeamMemberV4 createMember(
            ScenarioV4 scenario,
            StudentV4 student,
            ScenarioTeamV4 team
    ) {
        return ScenarioTeamMemberV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .team(team)
                .assignedAt(LocalDateTime.now())
                .assignedByType(ActorType.SYSTEM)
                .build();
    }

    @Transactional
    public void linkFireTeamAssignments(String scenarioId) {
        ScenarioTeamV4 fireTeam = scenarioTeamRepositoryV4
                .findByScenario_IdAndTeamCode(scenarioId, "FIRE")
                .orElse(null);

        if (fireTeam == null) {
            System.out.println("⚠ FIRE 팀 없음 → TEAM assignment 연결 스킵");
            return;
        }

        List<ScenarioAssignmentV4> assignments =
                scenarioAssignmentRepositoryV4.findByScenario_IdOrderByCreatedAtAsc(scenarioId);

        for (ScenarioAssignmentV4 assignment : assignments) {
            if (assignment.getTargetType() == null) {
                continue;
            }

            if (!"TEAM".equals(assignment.getTargetType().name())) {
                continue;
            }

            if (assignment.getContent() == null || assignment.getContent().getTitle() == null) {
                continue;
            }

            String title = assignment.getContent().getTitle();

            if (title.startsWith("소화팀:")) {
                assignment.setTargetTeam(fireTeam);
            }
        }

        scenarioAssignmentRepositoryV4.saveAll(assignments);
    }

    private String buildWarningMessage(ClassroomV4 classroom, ScenarioV4 scenario) {
        if (scenario.getScenarioType() == null || !"FIRE".equals(scenario.getScenarioType().name())) {
            return null;
        }

        if (classroom.getActiveMapVersion() == null) {
            return "활성 구조도가 설정되어 있지 않아 화재구역을 자동 매핑하지 못했습니다.";
        }

        if (scenario.getDisasterOriginElementId() == null || scenario.getDisasterOriginElementId().isBlank()) {
            return "활성 구조도에서 화재구역을 찾지 못했습니다.";
        }

        if (scenario.getSelectedScenarioEventContent() == null) {
            return "화재구역은 찾았지만 해당 장소에 맞는 시나리오 이벤트를 찾지 못했습니다.";
        }

        return null;
    }

    @Transactional
    public Map<String, Object> completeYoloExtinguisherMissionIfDetected(
            String classroomId,
            String studentId,
            String assignmentId,
            Map<String, Object> yoloResult
    ) {
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("studentId는 필수입니다.");
        }

        if (assignmentId == null || assignmentId.isBlank()) {
            throw new IllegalArgumentException("assignmentId는 필수입니다.");
        }

        boolean detected = isYoloDetected(yoloResult);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("detected", detected);

        if (!detected) {
            result.put("missionCompleted", false);
            result.put("itemAcquired", false);
            result.put("message", "소화기가 인식되지 않았습니다.");
            return result;
        }

        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        ScenarioV4 scenario = classroom.getActiveScenario();
        if (scenario == null) {
            throw new IllegalArgumentException("현재 활성 시나리오가 없습니다.");
        }

        StudentV4 student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        if (student.getClassroom() == null ||
                !student.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("해당 학생은 이 교실 소속이 아닙니다.");
        }

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("assignment가 존재하지 않습니다."));

        if (assignment.getScenario() == null ||
                !assignment.getScenario().getId().equals(scenario.getId())) {
            throw new IllegalArgumentException("해당 assignment는 현재 시나리오 소속이 아닙니다.");
        }

        String title = assignment.getContent() != null
                ? assignment.getContent().getTitle()
                : null;

        String missionCode = extractMissionCode(assignment.getParamsJson(), title);

        if (!"COMMON_FIND_EXTINGUISHER".equals(missionCode)
                && !"FIRETEAM_GET_EXTINGUISHER".equals(missionCode)) {
            throw new IllegalArgumentException("소화기 획득/찾기 미션이 아닙니다. missionCode=" + missionCode);
        }

        ItemV4 extinguisher = findOrCreateExtinguisherItem();

        boolean alreadyAcquired = studentItemRepositoryV4
                .findByScenario_IdAndStudent_IdAndItem_Id(
                        scenario.getId(),
                        student.getId(),
                        extinguisher.getId()
                )
                .isPresent();

        boolean alreadyMissionCompleted = studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenario.getId(),
                        assignment.getId(),
                        student.getId()
                )
                .map(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .orElse(false);

        if (!alreadyAcquired) {
            StudentItemV4 item = StudentItemV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .student(student)
                    .item(extinguisher)
                    .quantity(1)
                    .acquiredAt(LocalDateTime.now())
                    .acquiredSource(AcquiredSource.MISSION)
                    .isConsumed(false)
                    .build();

            studentItemRepositoryV4.save(item);
        }

        upsertStudentMissionProgressForRoom(
                scenario,
                assignment,
                student,
                1,
                1,
                ProgressStatus.COMPLETED
        );

        scenarioTriggerRepositoryV4
                .findByScenario_IdAndStudent_IdAndAssignment_Id(
                        scenario.getId(),
                        student.getId(),
                        assignment.getId()
                )
                .ifPresent(trigger -> {
                    trigger.setStatus("COMPLETED");
                    scenarioTriggerRepositoryV4.save(trigger);
                });

        if (!alreadyAcquired) {
            ScenarioActionEventV4 pickupEvent = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .classroom(classroom)
                    .student(student)
                    .actionType(ScenarioActionType.PICKUP_ITEM)
                    .valueText("FIRE_EXTINGUISHER")
                    .metaJson(buildYoloMissionMetaJson(assignment, missionCode, yoloResult, alreadyAcquired))
                    .createdAt(LocalDateTime.now())
                    .build();

            scenarioActionEventRepositoryV4.save(pickupEvent);
        }

        if (!alreadyMissionCompleted) {
            ScenarioActionEventV4 completeEvent = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .classroom(classroom)
                    .student(student)
                    .actionType(ScenarioActionType.MISSION_COMPLETE)
                    .valueText(missionCode)
                    .metaJson(buildYoloMissionMetaJson(assignment, missionCode, yoloResult, alreadyAcquired))
                    .createdAt(LocalDateTime.now())
                    .build();

            scenarioActionEventRepositoryV4.save(completeEvent);
        }

        result.put("missionCompleted", true);
        result.put("itemAcquired", !alreadyAcquired);
        result.put("alreadyAcquired", alreadyAcquired);
        result.put("alreadyMissionCompleted", alreadyMissionCompleted);
        result.put("missionCode", missionCode);
        result.put("assignmentId", assignment.getId());
        result.put("status", "COMPLETED");

        if (alreadyAcquired) {
            result.put("message", "이미 소화기를 획득한 학생입니다. 미션 완료 상태는 유지됩니다.");
        } else {
            result.put("message", "소화기를 인식하여 미션을 완료했습니다.");
        }

        return result;
    }

    private boolean isYoloDetected(Map<String, Object> yoloResult) {
        if (yoloResult == null) {
            return false;
        }

        Object detected = yoloResult.get("detected");

        if (detected instanceof Boolean bool) {
            return bool;
        }

        if (detected != null) {
            return Boolean.parseBoolean(String.valueOf(detected));
        }

        Object count = yoloResult.get("count");
        if (count instanceof Number number) {
            return number.intValue() > 0;
        }

        return false;
    }

    private ItemV4 findOrCreateExtinguisherItem() {
        return itemRepositoryV4.findByItemCode("FIRE_EXTINGUISHER")
                .orElseGet(() -> {
                    ItemV4 item = ItemV4.builder()
                            .id(UUID.randomUUID().toString())
                            .itemCode("FIRE_EXTINGUISHER")
                            .itemName("소화기")
                            .description("화재 진압 미션에 사용하는 소화기")
                            .itemType("EQUIPMENT")
                            .build();

                    return itemRepositoryV4.save(item);
                });
    }

    private void upsertStudentMissionProgressForRoom(
            ScenarioV4 scenario,
            ScenarioAssignmentV4 assignment,
            StudentV4 student,
            int requiredCount,
            int progressCount,
            ProgressStatus status
    ) {
        StudentMissionProgressV4 progress = studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenario.getId(),
                        assignment.getId(),
                        student.getId()
                )
                .orElseGet(() -> StudentMissionProgressV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .assignment(assignment)
                        .student(student)
                        .requiredCount(requiredCount)
                        .progressCount(0)
                        .status(ProgressStatus.IN_PROGRESS)
                        .startedAt(LocalDateTime.now())
                        .build());

        progress.setRequiredCount(requiredCount);
        progress.setProgressCount(Math.min(progressCount, requiredCount));
        progress.setStatus(status);
        progress.setUpdatedAt(LocalDateTime.now());

        if (status == ProgressStatus.COMPLETED && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }

        studentMissionProgressRepository.save(progress);
    }

    private String buildYoloMissionMetaJson(
            ScenarioAssignmentV4 assignment,
            String missionCode,
            Map<String, Object> yoloResult,
            boolean alreadyAcquired
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source", "YOLO");
            map.put("assignmentId", assignment.getId());
            map.put("missionCode", missionCode);
            map.put("alreadyAcquired", alreadyAcquired);
            map.put("yoloDetected", yoloResult != null ? yoloResult.get("detected") : null);
            map.put("yoloCount", yoloResult != null ? yoloResult.get("count") : null);
            map.put("best", yoloResult != null ? yoloResult.get("best") : null);

            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Transactional
    public ActiveMapResponse syncBeaconMappingsForActiveMap(String classroomId) {
        ClassroomV4 classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        /*
         * 운영 정책:
         * 훈련 중에는 수동 비콘-구역 매핑 재동기화를 막는다.
         */
        if (classroom.getTrainingState() == TrainingState.RUNNING) {
            throw new IllegalStateException("훈련 중에는 비콘 매핑을 재동기화할 수 없습니다.");
        }

        if (classroom.getActiveMapVersion() == null) {
            throw new IllegalArgumentException("활성 구조도가 존재하지 않습니다.");
        }

        BeaconAutoMappingService.SyncResult syncResult =
                beaconAutoMappingService.syncForActiveMap(classroom);

        return ActiveMapResponse.builder()
                .classroomId(classroom.getId())
                .activeMapVersionId(
                        classroom.getActiveMapVersion() != null
                                ? classroom.getActiveMapVersion().getId()
                                : null
                )
                .label(
                        classroom.getActiveMapVersion() != null
                                ? classroom.getActiveMapVersion().getLabel()
                                : null
                )
                .syncFloorsProcessed(syncResult.floorsProcessed())
                .syncBeaconsProcessed(syncResult.beaconsProcessed())
                .syncMappingsCreated(syncResult.mappingsCreated())
                .syncMappingsUpdated(syncResult.mappingsUpdated())
                .syncMappingsDeactivated(syncResult.mappingsDeactivated())
                .syncUnmatchedBeacons(syncResult.unmatchedBeacons())
                .build();
    }

}