package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.*;
import com.example.disaster_ar.dto.scenario.*;
import com.example.disaster_ar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentCallMissionService {

    private final StudentCallMissionRepositoryV4 studentCallMissionRepositoryV4;
    private final ScenarioRepository scenarioRepository;
    private final StudentRepositoryV4 studentRepositoryV4;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final StudentMissionProgressRepositoryV4 studentMissionProgressRepositoryV4;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void createSchedulesForTraining(
            ScenarioV4 scenario,
            ClassroomV4 classroom,
            List<StudentV4> students
    ) {
        if (scenario == null || classroom == null || students == null || students.isEmpty()) {
            return;
        }

        String trainingSessionId = classroom.getActiveTrainingSessionId();
        if (trainingSessionId == null || trainingSessionId.isBlank()) {
            return;
        }

        // 같은 scenario를 같은 방에서 새 훈련처럼 재사용하므로 이전 스케줄은 지운다.
        studentCallMissionRepositoryV4.deleteByScenario_Id(scenario.getId());

        ScenarioAssignmentV4 callAssignment = findCallAssignment(scenario.getId()).orElse(null);

        int trainTimeSeconds = scenario.getTrainTime() != null && scenario.getTrainTime() > 0
                ? scenario.getTrainTime()
                : 300;
        int interval = Math.max(1, trainTimeSeconds / (students.size() + 1));
        LocalDateTime startedAt = classroom.getTrainingStartedAt() != null
                ? classroom.getTrainingStartedAt()
                : LocalDateTime.now();
        LocalDateTime now = LocalDateTime.now();

        List<StudentCallMissionV4> missions = new ArrayList<>();
        for (int i = 0; i < students.size(); i++) {
            StudentV4 student = students.get(i);
            if (student == null || !trainingSessionId.equals(student.getTrainingSessionId())) {
                continue;
            }

            int offsetSeconds = interval * (i + 1);
            missions.add(StudentCallMissionV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .classroom(classroom)
                    .trainingSessionId(trainingSessionId)
                    .student(student)
                    .assignment(callAssignment)
                    .triggerOffsetSeconds(offsetSeconds)
                    .availableFrom(startedAt.plusSeconds(offsetSeconds))
                    .status(CallMissionStatus.SCHEDULED)
                    .createdAt(now)
                    .updatedAt(now)
                    .build());
        }

        studentCallMissionRepositoryV4.saveAll(missions);
    }

    @Transactional
    public StudentCallMissionResponse getStudentCallMission(String scenarioId, String studentId) {
        ScenarioV4 scenario = findScenario(scenarioId);
        ClassroomV4 classroom = requireActiveClassroom(scenario);
        StudentV4 student = findStudent(studentId);
        validateCurrentTrainingStudent(classroom, student);

        StudentCallMissionV4 mission = findCurrentMission(scenario, classroom, student);
        mission = refreshAvailability(mission);

        return toResponse(mission, LocalDateTime.now());
    }

    @Transactional
    public CallMissionStartResponse startStudentCallMission(String scenarioId, String studentId) {
        ScenarioV4 scenario = findScenario(scenarioId);
        ClassroomV4 classroom = requireActiveClassroom(scenario);
        if (classroom.getTrainingState() != TrainingState.RUNNING) {
            throw new IllegalStateException("훈련 중에만 전화 미션을 시작할 수 있습니다.");
        }
        StudentV4 student = findStudent(studentId);
        validateCurrentTrainingStudent(classroom, student);

        StudentCallMissionV4 mission = findCurrentMission(scenario, classroom, student);
        mission = refreshAvailability(mission);

        LocalDateTime now = LocalDateTime.now();

        long remainingSeconds = remainingSeconds(mission, now);

        if (remainingSeconds > 0) {
            return CallMissionStartResponse.builder()
                    .status("NOT_AVAILABLE_YET")
                    .mode(null)
                    .message("아직 전화 미션 시간이 아닙니다.")
                    .remainingSeconds(remainingSeconds)
                    .build();
        }

        if (mission.getStatus() == CallMissionStatus.CALLING) {
            if (Boolean.TRUE.equals(scenario.getTeacherCallEnabled())) {
                String phone = normalizePhoneNumber(scenario.getTeacherPhoneNumber());
                return CallMissionStartResponse.builder()
                        .status("CALL_READY")
                        .mode("TEACHER_CALL")
                        .message("이미 전화 연결이 시작되었습니다.")
                        .teacherPhoneNumber(phone)
                        .remainingSeconds(0L)
                        .build();
            }

            return CallMissionStartResponse.builder()
                    .status("QUIZ_READY")
                    .mode("QUIZ")
                    .message("이미 119 신고 미션이 시작되었습니다.")
                    .quizApi("/api/scenarios/" + scenarioId + "/call/quiz")
                    .remainingSeconds(0L)
                    .build();
        }

        if (mission.getStatus() == CallMissionStatus.WAITING_TEACHER_REVIEW) {
            throw new IllegalStateException("이미 통화가 완료되어 교사 판정을 기다리는 중입니다.");
        }

        if (mission.getStatus() == CallMissionStatus.SUCCESS || mission.getStatus() == CallMissionStatus.FAILED) {
            throw new IllegalStateException("이미 판정이 완료된 전화 미션입니다.");
        }

        if (mission.getStatus() == CallMissionStatus.EXPIRED) {
            throw new IllegalStateException("만료된 전화 미션입니다.");
        }

        mission.setStatus(CallMissionStatus.CALLING);
        mission.setCallStartedAt(now);
        mission.setUpdatedAt(now);
        studentCallMissionRepositoryV4.save(mission);

        saveActionEvent(scenario, classroom, student, ScenarioActionType.CALL_119, "CALL_STARTED", buildMeta(mission));

        if (Boolean.TRUE.equals(scenario.getTeacherCallEnabled())) {
            String phone = normalizePhoneNumber(scenario.getTeacherPhoneNumber());
            if (phone == null || phone.isBlank()) {
                throw new IllegalStateException("선생님 전화 받기가 ON이지만 전화번호가 없습니다.");
            }
            return CallMissionStartResponse.builder()
                    .status("CALL_READY")
                    .mode("TEACHER_CALL")
                    .message("선생님에게 전화 연결을 진행하세요.")
                    .teacherPhoneNumber(phone)
                    .remainingSeconds(0L)
                    .build();
        }

        return CallMissionStartResponse.builder()
                .status("QUIZ_READY")
                .mode("QUIZ")
                .message("119 신고 순서 퀴즈를 진행하세요.")
                .quizApi("/api/scenarios/" + scenarioId + "/call/quiz")
                .remainingSeconds(0L)
                .build();
    }

    @Transactional
    public StudentCallMissionResponse completeStudentCallMission(String scenarioId, String studentId) {
        ScenarioV4 scenario = findScenario(scenarioId);
        ClassroomV4 classroom = requireActiveClassroom(scenario);
        StudentV4 student = findStudent(studentId);
        validateCurrentTrainingStudent(classroom, student);

        StudentCallMissionV4 mission = findCurrentMission(scenario, classroom, student);
        LocalDateTime now = LocalDateTime.now();

        if (mission.getStatus() == CallMissionStatus.WAITING_TEACHER_REVIEW) {
            return toResponse(mission, now);
        }

        if (mission.getStatus() == CallMissionStatus.SUCCESS || mission.getStatus() == CallMissionStatus.FAILED) {
            throw new IllegalStateException("이미 교사 판정이 완료된 전화 미션입니다.");
        }

        if (mission.getStatus() != CallMissionStatus.CALLING) {
            throw new IllegalStateException("전화 연결이 시작된 상태에서만 통화 완료 처리할 수 있습니다.");
        }

        mission.setStatus(CallMissionStatus.WAITING_TEACHER_REVIEW);
        mission.setCallEndedAt(now);
        mission.setUpdatedAt(now);
        studentCallMissionRepositoryV4.save(mission);

        saveActionEvent(
                scenario,
                classroom,
                student,
                ScenarioActionType.CALL_119_END,
                "WAITING_TEACHER_REVIEW",
                buildMeta(mission)
        );

        return toResponse(mission, now);
    }

    @Transactional
    public CallMissionListResponse listCallMissions(String scenarioId) {
        ScenarioV4 scenario = findScenario(scenarioId);
        ClassroomV4 classroom = requireActiveClassroom(scenario);
        String trainingSessionId = classroom.getActiveTrainingSessionId();
        LocalDateTime now = LocalDateTime.now();

        List<StudentCallMissionResponse> missions = studentCallMissionRepositoryV4
                .findByScenario_IdAndTrainingSessionIdOrderByTriggerOffsetSecondsAsc(scenarioId, trainingSessionId)
                .stream()
                .map(this::refreshAvailability)
                .map(m -> toResponse(m, now))
                .toList();

        return CallMissionListResponse.builder()
                .scenarioId(scenarioId)
                .trainingSessionId(trainingSessionId)
                .missions(missions)
                .build();
    }

    private StudentCallMissionV4 refreshAvailability(StudentCallMissionV4 mission) {
        if (mission == null) {
            return null;
        }

        if (mission.getStatus() == CallMissionStatus.SCHEDULED
                && mission.getAvailableFrom() != null
                && !LocalDateTime.now().isBefore(mission.getAvailableFrom())) {

            mission.setStatus(CallMissionStatus.AVAILABLE);
            mission.setUpdatedAt(LocalDateTime.now());

            return studentCallMissionRepositoryV4.save(mission);
        }

        return mission;
    }

    @Transactional
    public StudentCallMissionResponse judgeCallMission(
            String scenarioId,
            String studentId,
            CallMissionJudgeRequest req
    ) {
        ScenarioV4 scenario = findScenario(scenarioId);
        ClassroomV4 classroom = requireActiveClassroom(scenario);
        StudentV4 student = findStudent(studentId);
        validateCurrentTrainingStudent(classroom, student);

        StudentCallMissionV4 mission = findCurrentMission(scenario, classroom, student);
        LocalDateTime now = LocalDateTime.now();

        if (mission.getStatus() == CallMissionStatus.SUCCESS || mission.getStatus() == CallMissionStatus.FAILED) {
            throw new IllegalStateException("이미 판정이 완료된 전화 미션입니다.");
        }

        if (mission.getStatus() != CallMissionStatus.WAITING_TEACHER_REVIEW) {
            throw new IllegalStateException("학생이 통화 완료를 한 뒤에만 교사 판정을 할 수 있습니다.");
        }

        boolean success = Boolean.TRUE.equals(req.getSuccess());

        mission.setStatus(success ? CallMissionStatus.SUCCESS : CallMissionStatus.FAILED);
        mission.setTeacherJudgedAt(now);
        mission.setTeacherJudgementMemo(req.getMemo());
        mission.setUpdatedAt(now);

        if (req.getTeacherUserId() != null && !req.getTeacherUserId().isBlank()) {
            userRepository.findById(req.getTeacherUserId()).ifPresent(mission::setTeacherJudgedBy);
        }

        studentCallMissionRepositoryV4.save(mission);

        saveActionEvent(
                scenario,
                classroom,
                student,
                success ? ScenarioActionType.MISSION_COMPLETE : ScenarioActionType.CALL_119_END,
                success ? "TEACHER_JUDGED_SUCCESS" : "TEACHER_JUDGED_FAILED",
                buildMeta(mission)
        );

        if (mission.getAssignment() != null) {
            upsertMissionProgress(
                    scenario,
                    mission.getAssignment(),
                    student,
                    success ? ProgressStatus.COMPLETED : ProgressStatus.FAILED
            );
        }

        return toResponse(mission, now);
    }

    @Transactional
    public void deleteByScenarioId(String scenarioId) {
        studentCallMissionRepositoryV4.deleteByScenario_Id(scenarioId);
    }

    private ScenarioV4 findScenario(String scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));
    }

    private StudentV4 findStudent(String studentId) {
        return studentRepositoryV4.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));
    }

    private ClassroomV4 requireActiveClassroom(ScenarioV4 scenario) {
        ClassroomV4 classroom = scenario.getClassroom();
        if (classroom == null) {
            throw new IllegalArgumentException("시나리오에 연결된 교실이 없습니다.");
        }
        if (classroom.getTrainingState() != TrainingState.RUNNING && classroom.getTrainingState() != TrainingState.ENDED) {
            throw new IllegalStateException("훈련이 시작되지 않았습니다.");
        }
        if (classroom.getActiveTrainingSessionId() == null || classroom.getActiveTrainingSessionId().isBlank()) {
            throw new IllegalStateException("현재 훈련 세션이 없습니다.");
        }
        return classroom;
    }

    private void validateCurrentTrainingStudent(ClassroomV4 classroom, StudentV4 student) {
        if (student.getClassroom() == null || !classroom.getId().equals(student.getClassroom().getId())) {
            throw new IllegalArgumentException("해당 학생은 이 교실 소속이 아닙니다.");
        }
        if (!classroom.getActiveTrainingSessionId().equals(student.getTrainingSessionId())) {
            throw new IllegalArgumentException("현재 훈련 세션 학생이 아닙니다.");
        }
        if (Boolean.TRUE.equals(student.getIsKicked())) {
            throw new IllegalArgumentException("퇴장 처리된 학생입니다.");
        }
    }

    private StudentCallMissionV4 findCurrentMission(ScenarioV4 scenario, ClassroomV4 classroom, StudentV4 student) {
        return studentCallMissionRepositoryV4
                .findByScenario_IdAndTrainingSessionIdAndStudent_Id(
                        scenario.getId(),
                        classroom.getActiveTrainingSessionId(),
                        student.getId()
                )
                .orElseThrow(() -> new IllegalArgumentException("현재 훈련 세션의 전화 미션이 없습니다."));
    }

    private Optional<ScenarioAssignmentV4> findCallAssignment(String scenarioId) {
        return scenarioAssignmentRepositoryV4.findByScenario_IdOrderByCreatedAtAsc(scenarioId)
                .stream()
                .filter(a -> "COMMON_REPORT_CALL".equals(extractMissionCode(a)))
                .findFirst();
    }

    private String extractMissionCode(ScenarioAssignmentV4 assignment) {
        if (assignment == null) {
            return null;
        }
        String paramsJson = assignment.getParamsJson();
        if (paramsJson != null && !paramsJson.isBlank()) {
            try {
                Map<?, ?> map = objectMapper.readValue(paramsJson, Map.class);
                Object code = map.get("missionCode");
                if (code != null) {
                    return String.valueOf(code);
                }
            } catch (Exception ignored) {
            }
        }
        ContentV4 content = assignment.getContent();
        if (content != null && content.getTitle() != null && content.getTitle().contains("119")) {
            return "COMMON_REPORT_CALL";
        }
        return null;
    }

    private StudentCallMissionResponse toResponse(StudentCallMissionV4 mission, LocalDateTime now) {
        long remainingSeconds = remainingSeconds(mission, now);
        boolean callAvailable = remainingSeconds <= 0
                && (
                mission.getStatus() == CallMissionStatus.SCHEDULED
                        || mission.getStatus() == CallMissionStatus.AVAILABLE
        );

        return StudentCallMissionResponse.builder()
                .missionId(mission.getId())
                .scenarioId(mission.getScenario() != null ? mission.getScenario().getId() : null)
                .classroomId(mission.getClassroom() != null ? mission.getClassroom().getId() : null)
                .trainingSessionId(mission.getTrainingSessionId())
                .studentId(mission.getStudent() != null ? mission.getStudent().getId() : null)
                .studentName(mission.getStudent() != null ? mission.getStudent().getStudentName() : null)
                .assignmentId(mission.getAssignment() != null ? mission.getAssignment().getId() : null)
                .triggerOffsetSeconds(mission.getTriggerOffsetSeconds())
                .availableFrom(mission.getAvailableFrom())
                .status(mission.getStatus() != null ? mission.getStatus().name() : null)
                .callAvailable(callAvailable)
                .remainingSeconds(Math.max(remainingSeconds, 0))
                .callStartedAt(mission.getCallStartedAt())
                .callEndedAt(mission.getCallEndedAt())
                .teacherJudgedAt(mission.getTeacherJudgedAt())
                .teacherJudgementMemo(mission.getTeacherJudgementMemo())
                .build();
    }

    private long remainingSeconds(StudentCallMissionV4 mission, LocalDateTime now) {
        if (mission.getAvailableFrom() == null || now == null) {
            return 0L;
        }
        return Duration.between(now, mission.getAvailableFrom()).getSeconds();
    }

    private void saveActionEvent(
            ScenarioV4 scenario,
            ClassroomV4 classroom,
            StudentV4 student,
            ScenarioActionType actionType,
            String valueText,
            String metaJson
    ) {
        scenarioActionEventRepositoryV4.save(ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .classroom(classroom)
                .student(student)
                .actionType(actionType)
                .valueText(valueText)
                .metaJson(metaJson)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void upsertMissionProgress(
            ScenarioV4 scenario,
            ScenarioAssignmentV4 assignment,
            StudentV4 student,
            ProgressStatus status
    ) {
        StudentMissionProgressV4 progress = studentMissionProgressRepositoryV4
                .findByScenario_IdAndAssignment_IdAndStudent_Id(scenario.getId(), assignment.getId(), student.getId())
                .orElseGet(() -> StudentMissionProgressV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .assignment(assignment)
                        .student(student)
                        .requiredCount(1)
                        .progressCount(0)
                        .status(ProgressStatus.IN_PROGRESS)
                        .startedAt(LocalDateTime.now())
                        .build());

        progress.setRequiredCount(1);
        progress.setProgressCount(status == ProgressStatus.COMPLETED ? 1 : 0);
        progress.setStatus(status);
        progress.setUpdatedAt(LocalDateTime.now());
        if (status == ProgressStatus.COMPLETED && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        studentMissionProgressRepositoryV4.save(progress);
    }

    private String buildMeta(StudentCallMissionV4 mission) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("callMissionId", mission.getId());
            map.put("trainingSessionId", mission.getTrainingSessionId());
            map.put("triggerOffsetSeconds", mission.getTriggerOffsetSeconds());
            map.put("availableFrom", mission.getAvailableFrom() != null ? mission.getAvailableFrom().toString() : null);
            map.put("status", mission.getStatus() != null ? mission.getStatus().name() : null);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String normalizePhoneNumber(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.replaceAll("[^0-9+]", "");
        return normalized.isBlank() ? null : normalized;
    }
}
