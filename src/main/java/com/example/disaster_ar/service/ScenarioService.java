package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.*;
import com.example.disaster_ar.dto.scenario.*;
import com.example.disaster_ar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.disaster_ar.domain.v4.ContentV4;
import com.example.disaster_ar.domain.v4.enums.ContentType;
import com.example.disaster_ar.dto.scenario.RandomQuizResponse;
import com.example.disaster_ar.repository.ContentRepository;
import com.example.disaster_ar.dto.evaluation.StudentScoreBreakdown;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ClassroomRepository classroomRepository;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;
    private final StudentRepositoryV4 studentRepository;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final StudentMissionProgressRepositoryV4 studentMissionProgressRepository;
    private final StudentBeaconEventRepositoryV4 studentBeaconEventRepositoryV4;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final ScenarioTriggerRepositoryV4 scenarioTriggerRepositoryV4;
    private final QuizSubmissionRepositoryV4 quizSubmissionRepositoryV4;
    private final CardQuizSubmissionRepositoryV4 cardQuizSubmissionRepositoryV4;
    private final EvaluationRepositoryV4 evaluationRepositoryV4;
    private final TeamMissionStepProgressRepositoryV4 teamMissionStepProgressRepositoryV4;
    private final TeamMissionProgressRepositoryV4 teamMissionProgressRepositoryV4;
    private final ContentRepository contentRepository;
    private final StudentRepositoryV4 studentRepositoryV4;
    private final ScenarioTeamRepositoryV4 scenarioTeamRepositoryV4;
    private final ScenarioTeamMemberRepositoryV4 scenarioTeamMemberRepositoryV4;
    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;
    private final EvaluationScoreService evaluationScoreService;
    private final ObjectMapper objectMapper;


    public ScenarioResponse create(ScenarioCreateRequest req) {
        ClassroomV4 classroom = classroomRepository.findById(req.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        ScenarioV4 s = new ScenarioV4();
        if (s.getId() == null) s.setId(UUID.randomUUID().toString());

        s.setClassroom(classroom);
        s.setScenarioName(req.getScenarioName());
        s.setScenarioType(parseEnum(ScenarioType.class, req.getScenarioType(), "scenarioType"));

        if (req.getTriggerMode() != null) {
            s.setTriggerMode(parseEnum(TriggerMode.class, req.getTriggerMode(), "triggerMode"));
        }
        if (req.getTeamMode() != null) {
            s.setTeamMode(parseEnum(TeamMode.class, req.getTeamMode(), "teamMode"));
        }
        if (req.getNpcMode() != null) {
            s.setNpcMode(parseEnum(NpcMode.class, req.getNpcMode(), "npcMode"));
        }

        s.setLocation(req.getLocation());
        s.setIntensity(req.getIntensity());
        s.setTrainTime(req.getTrainTime());
        s.setTeamAssignmentJson(req.getTeamAssignment());
        s.setNpcPositionsJson(req.getNpcPositions());
        s.setParticipantCount(req.getParticipantCount());
        s.setCreatedTime(LocalDateTime.now());

        scenarioRepository.save(s);
        return toDto(s);
    }

    public ScenarioResponse update(ScenarioUpdateRequest req) {
        ScenarioV4 s = scenarioRepository.findById(req.getScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        if (req.getScenarioName() != null) s.setScenarioName(req.getScenarioName());
        if (req.getScenarioType() != null) {
            s.setScenarioType(parseEnum(ScenarioType.class, req.getScenarioType(), "scenarioType"));
        }
        if (req.getTriggerMode() != null) {
            s.setTriggerMode(parseEnum(TriggerMode.class, req.getTriggerMode(), "triggerMode"));
        }
        if (req.getTeamMode() != null) {
            s.setTeamMode(parseEnum(TeamMode.class, req.getTeamMode(), "teamMode"));
        }
        if (req.getNpcMode() != null) {
            s.setNpcMode(parseEnum(NpcMode.class, req.getNpcMode(), "npcMode"));
        }

        if (req.getLocation() != null) s.setLocation(req.getLocation());
        if (req.getIntensity() != null) s.setIntensity(req.getIntensity());
        if (req.getTrainTime() != null) s.setTrainTime(req.getTrainTime());
        if (req.getTeamAssignment() != null) s.setTeamAssignmentJson(req.getTeamAssignment());
        if (req.getNpcPositions() != null) s.setNpcPositionsJson(req.getNpcPositions());
        if (req.getParticipantCount() != null) s.setParticipantCount(req.getParticipantCount());

        scenarioRepository.save(s);
        return toDto(s);
    }

    public List<ScenarioResponse> listByClassroom(String classroomId) {
        return scenarioRepository.findByClassroom_IdOrderByCreatedTimeDesc(classroomId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ScenarioResponse toDto(ScenarioV4 s) {
        ScenarioResponse r = new ScenarioResponse();
        r.setId(s.getId());
        r.setClassroomId(s.getClassroom().getId());
        r.setScenarioName(s.getScenarioName());
        r.setScenarioType(s.getScenarioType() != null ? s.getScenarioType().name() : null);
        r.setTriggerMode(s.getTriggerMode() != null ? s.getTriggerMode().name() : null);
        r.setTeamMode(s.getTeamMode() != null ? s.getTeamMode().name() : null);
        r.setNpcMode(s.getNpcMode() != null ? s.getNpcMode().name() : null);
        r.setLocation(s.getLocation());
        r.setIntensity(s.getIntensity());
        r.setTrainTime(s.getTrainTime());
        r.setTeamAssignmentJson(s.getTeamAssignmentJson());
        r.setNpcPositionsJson(s.getNpcPositionsJson());
        r.setParticipantCount(s.getParticipantCount());
        r.setCreatedTime(s.getCreatedTime());
        return r;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String fieldName) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 값 오류: " + raw);
        }
    }

    public ScenarioActionEventResponse createActionEvent(String scenarioId, ScenarioActionEventRequest req) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        ClassroomV4 classroom = null;
        if (req.getClassroomId() != null && !req.getClassroomId().isBlank()) {
            classroom = classroomRepository.findById(req.getClassroomId())
                    .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));
        }

        StudentV4 student = null;
        if (req.getStudentId() != null && !req.getStudentId().isBlank()) {
            student = studentRepository.findById(req.getStudentId())
                    .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));
        }

        BeaconV4 beacon = null;
        if (req.getBeaconId() != null && !req.getBeaconId().isBlank()) {
            beacon = beaconRepositoryV4.findById(req.getBeaconId())
                    .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));
        }

        ScenarioActionType actionType;
        try {
            actionType = ScenarioActionType.valueOf(req.getActionType().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 actionType 입니다.");
        }

        ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .classroom(classroom)
                .student(student)
                .actionType(actionType)
                .floorIndex(req.getFloorIndex())
                .elementId(req.getElementId())
                .beacon(beacon)
                .valueInt(req.getValueInt())
                .valueText(req.getValueText())
                .metaJson(req.getMetaJson())
                .createdAt(LocalDateTime.now())
                .build();

        scenarioActionEventRepositoryV4.save(event);

        return ScenarioActionEventResponse.builder()
                .actionEventId(event.getId())
                .scenarioId(scenario.getId())
                .studentId(student != null ? student.getId() : null)
                .actionType(event.getActionType().name())
                .createdAt(event.getCreatedAt())
                .build();
    }

    public void startCallMission(String scenarioId, CallMissionRequest req) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.CALL_119)
                .createdAt(LocalDateTime.now())
                .build();

        scenarioActionEventRepositoryV4.save(event);
    }

    public void endCallMission(String scenarioId, CallMissionRequest req) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        ScenarioActionEventV4 endEvent = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.CALL_119_END)
                .createdAt(LocalDateTime.now())
                .build();

        scenarioActionEventRepositoryV4.save(endEvent);

        if (Boolean.TRUE.equals(req.getSuccess())) {
            ScenarioActionEventV4 completeEvent = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .student(student)
                    .actionType(ScenarioActionType.MISSION_COMPLETE)
                    .createdAt(LocalDateTime.now())
                    .build();

            scenarioActionEventRepositoryV4.save(completeEvent);

            StudentMissionProgressV4 progress = studentMissionProgressRepository
                    .findByScenario_IdAndAssignment_IdAndStudent_Id(
                            scenarioId,
                            req.getAssignmentId(),
                            student.getId()
                    )
                    .orElseThrow(() -> new IllegalArgumentException("미션 진행 정보 없음"));

            progress.setStatus(ProgressStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
            progress.setProgressCount(progress.getRequiredCount());

            studentMissionProgressRepository.save(progress);
        }
    }

    public List<TriggeredAssignmentResponse> getTriggeredAssignments(
            String scenarioId,
            String studentId
    ) {
        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        return scenarioTriggerRepositoryV4
                .findByScenario_IdAndStudent_IdOrderByTriggeredAtDesc(scenarioId, studentId)
                .stream()
                .map(trigger -> {
                    ScenarioAssignmentV4 assignment = trigger.getAssignment();
                    ContentV4 content = assignment.getContent();

                    return TriggeredAssignmentResponse.builder()
                            .triggerId(trigger.getId())
                            .assignmentId(assignment.getId())
                            .assignmentType(
                                    assignment.getAssignmentType() != null
                                            ? assignment.getAssignmentType().name()
                                            : null
                            )
                            .status(trigger.getStatus())
                            .triggerReason(
                                    trigger.getTriggerReason() != null
                                            ? trigger.getTriggerReason().name()
                                            : null
                            )
                            .triggeredAt(trigger.getTriggeredAt())
                            .contentId(content != null ? content.getId() : null)
                            .title(content != null ? content.getTitle() : null)
                            .description(content != null ? content.getDescription() : null)
                            .floorIndex(assignment.getFloorIndex())
                            .elementId(assignment.getElementId())
                            .beaconId(
                                    assignment.getBeacon() != null
                                            ? assignment.getBeacon().getId()
                                            : null
                            )
                            .build();
                })
                .toList();
    }

    public MissionCompleteResponse completeMission(
            String scenarioId,
            String assignmentId,
            MissionCompleteRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        StudentMissionProgressV4 progress = studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenarioId,
                        assignmentId,
                        student.getId()
                )
                .orElseThrow(() -> new IllegalArgumentException("미션 진행 정보 없음"));

        if (progress.getStatus() != ProgressStatus.COMPLETED) {
            progress.setStatus(ProgressStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
            progress.setProgressCount(progress.getRequiredCount());
            studentMissionProgressRepository.save(progress);

            ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .student(student)
                    .actionType(ScenarioActionType.MISSION_COMPLETE)
                    .createdAt(LocalDateTime.now())
                    .build();

            scenarioActionEventRepositoryV4.save(event);
        }

        return MissionCompleteResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignmentId)
                .studentId(student.getId())
                .status(progress.getStatus().name())
                .completedAt(progress.getCompletedAt())
                .build();
    }

    public MissionProgressResponse progressMission(
            String scenarioId,
            String assignmentId,
            MissionProgressRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        StudentMissionProgressV4 progress = studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenarioId,
                        assignmentId,
                        student.getId()
                )
                .orElseThrow(() -> new IllegalArgumentException("미션 진행 정보 없음"));

        int increment = (req.getIncrementCount() == null || req.getIncrementCount() < 1)
                ? 1
                : req.getIncrementCount();

        int nextCount = progress.getProgressCount() + increment;
        if (nextCount > progress.getRequiredCount()) {
            nextCount = progress.getRequiredCount();
        }

        ProgressStatus beforeStatus = progress.getStatus();

        progress.setProgressCount(nextCount);
        progress.setUpdatedAt(LocalDateTime.now());

        if (beforeStatus != ProgressStatus.COMPLETED && nextCount >= progress.getRequiredCount()) {
            progress.setStatus(ProgressStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
        }

        studentMissionProgressRepository.save(progress);

        ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(
                        progress.getStatus() == ProgressStatus.COMPLETED && beforeStatus != ProgressStatus.COMPLETED
                                ? ScenarioActionType.MISSION_COMPLETE
                                : ScenarioActionType.MISSION_INCREMENT
                )
                .valueInt(increment)
                .createdAt(LocalDateTime.now())
                .build();

        scenarioActionEventRepositoryV4.save(event);

        return MissionProgressResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignmentId)
                .studentId(student.getId())
                .requiredCount(progress.getRequiredCount())
                .progressCount(progress.getProgressCount())
                .status(progress.getStatus().name())
                .build();
    }

    public QuizSubmitResponse submitQuiz(
            String scenarioId,
            String assignmentId,
            QuizSubmitRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("assignment 없음"));

        if (!assignment.getScenario().getId().equals(scenario.getId())) {
            throw new IllegalArgumentException("해당 assignment는 이 시나리오 소속이 아닙니다.");
        }

        if (req.getContentId() == null || req.getContentId().isBlank()) {
            throw new IllegalArgumentException("랜덤퀴즈 제출에는 contentId가 필요합니다.");
        }

        ContentV4 content = contentRepository.findById(req.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("퀴즈 content가 존재하지 않습니다."));

        if (content.getContentType() != ContentType.QUIZ) {
            throw new IllegalArgumentException("해당 content는 QUIZ 타입이 아닙니다.");
        }

        if (content.getAnswer() == null) {
            throw new IllegalArgumentException("객관식/OX 정답이 설정되어 있지 않습니다.");
        }

        Integer selectedAnswer = req.getSelectedAnswer();

        if (selectedAnswer == null) {
            throw new IllegalArgumentException("선택한 답안이 없습니다.");
        }

        Integer correctAnswer = content.getAnswer();
        boolean isCorrect = correctAnswer.equals(selectedAnswer);

        LocalDateTime now = LocalDateTime.now();

        QuizSubmissionV4 submission = quizSubmissionRepositoryV4
                .findByScenario_IdAndAssignment_IdAndStudent_IdAndContent_Id(
                        scenarioId,
                        assignmentId,
                        student.getId(),
                        content.getId()
                )
                .orElseGet(() -> QuizSubmissionV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .assignment(assignment)
                        .student(student)
                        .content(content)
                        .build());

        submission.setContent(content);
        submission.setSelectedAnswer(selectedAnswer);
        submission.setIsCorrect(isCorrect);
        submission.setStatus(isCorrect ? QuizResultStatus.CORRECT : QuizResultStatus.FAILED);
        submission.setSubmittedAt(now);

        quizSubmissionRepositoryV4.save(submission);

        ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.QUIZ_SUBMIT)
                .valueInt(selectedAnswer)
                .valueText(isCorrect ? "CORRECT" : "FAILED")
                .metaJson(buildQuizSubmitMetaJson(content, assignment))
                .createdAt(now)
                .build();

        scenarioActionEventRepositoryV4.save(event);

        int submittedCount = (int) quizSubmissionRepositoryV4
                .countByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenarioId,
                        assignmentId,
                        student.getId()
                );

        int correctCount = (int) quizSubmissionRepositoryV4
                .countByScenario_IdAndAssignment_IdAndStudent_IdAndIsCorrectTrue(
                        scenarioId,
                        assignmentId,
                        student.getId()
                );

        int requiredCorrectCount = getIntParam(assignment.getParamsJson(), "requiredCorrectCount", 3);
        int totalQuizCount = getIntParam(assignment.getParamsJson(), "totalQuizCount", 5);

        boolean missionCompleted = correctCount >= requiredCorrectCount;

        if (missionCompleted) {
            upsertStudentMissionProgress(
                    scenario,
                    assignment,
                    student,
                    requiredCorrectCount,
                    requiredCorrectCount,
                    ProgressStatus.COMPLETED
            );

            scenarioTriggerRepositoryV4
                    .findByScenario_IdAndStudent_IdAndAssignment_Id(
                            scenarioId,
                            student.getId(),
                            assignmentId
                    )
                    .ifPresent(trigger -> {
                        trigger.setStatus("COMPLETED");
                        scenarioTriggerRepositoryV4.save(trigger);
                    });
        } else {
            upsertStudentMissionProgress(
                    scenario,
                    assignment,
                    student,
                    requiredCorrectCount,
                    correctCount,
                    ProgressStatus.IN_PROGRESS
            );
        }

        return QuizSubmitResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .contentId(content.getId())
                .selectedAnswer(selectedAnswer)
                .correctAnswer(correctAnswer)
                .isCorrect(isCorrect)
                .status(submission.getStatus().name())
                .submittedAt(submission.getSubmittedAt())
                .submittedCount(submittedCount)
                .correctCount(correctCount)
                .requiredCorrectCount(requiredCorrectCount)
                .totalQuizCount(totalQuizCount)
                .missionCompleted(missionCompleted)
                .build();
    }

    private void upsertStudentMissionProgress(
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

    private String buildQuizSubmitMetaJson(ContentV4 content, ScenarioAssignmentV4 assignment) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("contentId", content.getId());
            map.put("quizType", content.getQuizType() != null ? content.getQuizType().name() : null);
            map.put("assignmentId", assignment.getId());

            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private int getIntParam(String paramsJson, String key, int defaultValue) {
        if (paramsJson == null || paramsJson.isBlank()) {
            return defaultValue;
        }

        try {
            Map<?, ?> map = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(paramsJson, Map.class);

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

    public CardQuizSubmitResponse submitCardQuiz(
            String scenarioId,
            String assignmentId,
            CardQuizSubmitRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("assignment 없음"));

        CardQuizKind quizKindEnum;
        try {
            quizKindEnum = CardQuizKind.valueOf(req.getQuizKind().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 quizKind 입니다.");
        }

        boolean isCorrect = false;
        if (assignment.getContent() != null && assignment.getContent().getAnswerJson() != null) {
            isCorrect = assignment.getContent().getAnswerJson().equals(req.getSelectedOrderJson());
        }

        LocalDateTime now = LocalDateTime.now();

        CardQuizSubmissionV4 submission = cardQuizSubmissionRepositoryV4
                .findByScenario_IdAndAssignment_IdAndStudent_IdAndQuizKind(
                        scenarioId,
                        assignmentId,
                        student.getId(),
                        quizKindEnum
                )
                .orElseGet(() -> CardQuizSubmissionV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .assignment(assignment)
                        .student(student)
                        .quizKind(quizKindEnum)
                        .build());

        submission.setSelectedOrderJson(req.getSelectedOrderJson());
        submission.setIsCorrect(isCorrect);
        submission.setSubmittedAt(now);

        cardQuizSubmissionRepositoryV4.save(submission);

        ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.CARD_QUIZ_SUBMIT)
                .valueText(isCorrect ? "CORRECT" : "FAILED")
                .createdAt(now)
                .build();

        scenarioActionEventRepositoryV4.save(event);

        return CardQuizSubmitResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .quizKind(quizKindEnum.name())
                .isCorrect(isCorrect)
                .submittedAt(now)
                .build();
    }

    @Transactional
    public ScenarioEvaluateResponse evaluateScenario(String scenarioId) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        /*
         * A안:
         * 같은 시나리오를 재평가할 때 기존 평가 결과를 삭제하고
         * 최신 기준으로 다시 생성한다.
         */
        evaluationRepositoryV4.deleteByScenario_Id(scenarioId);

        List<StudentV4> students = studentRepository.findByClassroom_IdOrderByJoinedAtAsc(
                scenario.getClassroom().getId()
        );

        List<ScenarioEvaluateResponse.StudentEvaluationItem> studentResults = new ArrayList<>();

        double totalScore = 0.0;
        int counted = 0;

        for (StudentV4 student : students) {
            StudentScoreBreakdown breakdown =
                    evaluationScoreService.calculateStudentScore(
                            scenarioId,
                            student.getId()
                    );

            double score = breakdown.getTotal() != null ? breakdown.getTotal() : 0.0;

            /*
             * 기존 정책 유지:
             * 퇴출 학생이면 -20점.
             */
            if (Boolean.TRUE.equals(student.getIsKicked())) {
                score -= 20.0;
            }

            if (score < 0) {
                score = 0.0;
            }

            breakdown.setTotal(score);

            String scoreJson = writeJsonSafely(Map.of(
                    "quiz", breakdown.getQuiz(),
                    "role", breakdown.getRole(),
                    "personal", breakdown.getPersonal(),
                    "safezone", breakdown.getSafezone(),
                    "total", breakdown.getTotal()
            ));

            String detailsJson = writeJsonSafely(breakdown);

            String feedback = buildEvaluationFeedback(breakdown, student);

            EvaluationV4 studentEval = EvaluationV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .level(EvalLevel.STUDENT)
                    .student(student)
                    .scoreTotal(score)
                    .scoreJson(scoreJson)
                    .detailsJson(detailsJson)
                    .feedbackText(feedback)
                    .createdAt(LocalDateTime.now())
                    .build();

            evaluationRepositoryV4.save(studentEval);

            studentResults.add(
                    ScenarioEvaluateResponse.StudentEvaluationItem.builder()
                            .studentId(student.getId())
                            .scoreTotal(score)
                            .quizScore(breakdown.getQuiz())
                            .roleScore(breakdown.getRole())
                            .personalScore(breakdown.getPersonal())
                            .safezoneScore(breakdown.getSafezone())
                            .correctQuizCount(breakdown.getCorrectQuizCount())
                            .feedbackText(feedback)
                            .build()
            );

            totalScore += score;
            counted++;
        }

        double scenarioScore = counted > 0 ? totalScore / counted : 0.0;

        String scenarioScoreJson = writeJsonSafely(Map.of(
                "average", scenarioScore,
                "studentCount", counted,
                "totalStudentScore", totalScore
        ));

        String scenarioDetailsJson = writeJsonSafely(Map.of(
                "evaluatedStudentCount", counted,
                "averageScore", scenarioScore,
                "totalStudentScore", totalScore
        ));

        EvaluationV4 scenarioEval = EvaluationV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .level(EvalLevel.SCENARIO)
                .scoreTotal(scenarioScore)
                .scoreJson(scenarioScoreJson)
                .detailsJson(scenarioDetailsJson)
                .feedbackText("학생 " + counted + "명 평균 점수")
                .createdAt(LocalDateTime.now())
                .build();

        evaluationRepositoryV4.save(scenarioEval);

        return ScenarioEvaluateResponse.builder()
                .scenarioId(scenario.getId())
                .scenarioScore(scenarioScore)
                .evaluatedStudentCount(counted)
                .studentEvaluations(studentResults)
                .build();
    }

    private String buildEvaluationFeedback(
            StudentScoreBreakdown breakdown,
            StudentV4 student
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("퀴즈 ")
                .append(breakdown.getQuiz())
                .append("점");

        sb.append(", 역할 ")
                .append(breakdown.getRole())
                .append("점");

        sb.append(", 개인 ")
                .append(breakdown.getPersonal())
                .append("점");

        sb.append(", 안전구역 ")
                .append(breakdown.getSafezone())
                .append("점");

        sb.append(", 정답 퀴즈 ")
                .append(breakdown.getCorrectQuizCount())
                .append("개");

        if (Boolean.TRUE.equals(student.getIsKicked())) {
            sb.append(", 퇴출 패널티 -20점");
        }

        return sb.toString();
    }

    private String writeJsonSafely(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    public ScenarioEvaluationDetailResponse getEvaluations(String scenarioId) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        EvaluationV4 scenarioEval = evaluationRepositoryV4
                .findTopByScenario_IdAndLevelOrderByCreatedAtDesc(
                        scenarioId,
                        EvalLevel.SCENARIO
                )
                .orElse(null);

        List<EvaluationV4> studentEvals = evaluationRepositoryV4
                .findByScenario_IdAndLevelOrderByCreatedAtDesc(
                        scenarioId,
                        EvalLevel.STUDENT
                );

        ScenarioEvaluationDetailResponse.EvaluationSummary scenarioSummary = null;
        if (scenarioEval != null) {
            scenarioSummary = ScenarioEvaluationDetailResponse.EvaluationSummary.builder()
                    .evaluationId(scenarioEval.getId())
                    .scoreTotal(scenarioEval.getScoreTotal())
                    .scoreJson(scenarioEval.getScoreJson())
                    .detailsJson(scenarioEval.getDetailsJson())
                    .feedbackText(scenarioEval.getFeedbackText())
                    .createdAt(scenarioEval.getCreatedAt())
                    .build();
        }

        List<ScenarioEvaluationDetailResponse.StudentEvaluationSummary> studentSummaries =
                studentEvals.stream()
                        .map(eval -> ScenarioEvaluationDetailResponse.StudentEvaluationSummary.builder()
                                .evaluationId(eval.getId())
                                .studentId(eval.getStudent() != null ? eval.getStudent().getId() : null)
                                .scoreTotal(eval.getScoreTotal())
                                .scoreJson(eval.getScoreJson())
                                .detailsJson(eval.getDetailsJson())
                                .feedbackText(eval.getFeedbackText())
                                .createdAt(eval.getCreatedAt())
                                .build())
                        .toList();

        return ScenarioEvaluationDetailResponse.builder()
                .scenarioId(scenario.getId())
                .scenarioEvaluation(scenarioSummary)
                .studentEvaluations(studentSummaries)
                .build();
    }

    public void completeTeamStep(
            String scenarioId,
            String assignmentId,
            Integer stepOrder,
            TeamStepCompleteRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        TeamMissionStepProgressV4 currentStep =
                teamMissionStepProgressRepositoryV4
                        .findByScenario_IdAndAssignment_IdAndTeam_IdAndStepOrder(
                                scenarioId,
                                assignmentId,
                                req.getTeamId(),
                                stepOrder
                        )
                        .orElseThrow(() -> new IllegalArgumentException("현재 step 없음"));

        if (currentStep.getStatus() == TeamStepStatus.COMPLETED) {
            return;
        }

        currentStep.setStatus(TeamStepStatus.COMPLETED);
        currentStep.setCompletedAt(LocalDateTime.now());
        teamMissionStepProgressRepositoryV4.save(currentStep);

        // 다음 step unlock
        teamMissionStepProgressRepositoryV4
                .findByScenario_IdAndAssignment_IdAndTeam_IdAndStepOrder(
                        scenarioId,
                        assignmentId,
                        req.getTeamId(),
                        stepOrder + 1
                )
                .ifPresent(nextStep -> {
                    if (nextStep.getStatus() == TeamStepStatus.LOCKED) {
                        nextStep.setStatus(TeamStepStatus.IN_PROGRESS);
                        nextStep.setStartedAt(LocalDateTime.now());
                        teamMissionStepProgressRepositoryV4.save(nextStep);
                    }
                });

        // 전체 step 완료 체크
        boolean allCompleted = teamMissionStepProgressRepositoryV4.findAll()
                .stream()
                .filter(s ->
                        s.getScenario().getId().equals(scenarioId) &&
                                s.getAssignment().getId().equals(assignmentId) &&
                                s.getTeam().getId().equals(req.getTeamId())
                )
                .allMatch(s -> s.getStatus() == TeamStepStatus.COMPLETED);

        if (allCompleted) {
            TeamMissionProgressV4 mission =
                    teamMissionProgressRepositoryV4
                            .findByScenario_IdAndAssignment_IdAndTeam_Id(
                                    scenarioId,
                                    assignmentId,
                                    req.getTeamId()
                            )
                            .orElseThrow(() -> new IllegalArgumentException("팀 미션 없음"));

            mission.setStatus(ProgressStatus.COMPLETED);
            mission.setCompletedAt(LocalDateTime.now());
            teamMissionProgressRepositoryV4.save(mission);

            // 로그
            ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .actionType(ScenarioActionType.MISSION_COMPLETE)
                    .createdAt(LocalDateTime.now())
                    .build();

            scenarioActionEventRepositoryV4.save(event);
        }
    }

    public TeamStepStatusResponse getTeamSteps(
            String scenarioId,
            String assignmentId,
            String teamId
    ) {
        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        List<TeamMissionStepProgressV4> steps =
                teamMissionStepProgressRepositoryV4
                        .findByScenario_IdAndAssignment_IdAndTeam_IdOrderByStepOrderAsc(
                                scenarioId,
                                assignmentId,
                                teamId
                        );

        List<TeamStepStatusResponse.StepItem> stepItems =
                steps.stream()
                        .map(s -> TeamStepStatusResponse.StepItem.builder()
                                .stepOrder(s.getStepOrder())
                                .stepType(s.getStepType())
                                .status(s.getStatus().name())
                                .startedAt(s.getStartedAt())
                                .completedAt(s.getCompletedAt())
                                .build())
                        .toList();

        return TeamStepStatusResponse.builder()
                .scenarioId(scenarioId)
                .assignmentId(assignmentId)
                .teamId(teamId)
                .steps(stepItems)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getScenarioTriggers(String scenarioId, String studentId) {

        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        List<ScenarioTriggerV4> triggers;

        if (studentId != null && !studentId.isBlank()) {
            triggers = scenarioTriggerRepositoryV4
                    .findByScenario_IdAndStudent_IdOrderByTriggeredAtDesc(scenarioId, studentId);
        } else {
            triggers = scenarioTriggerRepositoryV4
                    .findByScenario_IdOrderByTriggeredAtDesc(scenarioId);
        }

        return triggers.stream().map(t -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("triggerId", t.getId());
            map.put("scenarioId", t.getScenario() != null ? t.getScenario().getId() : null);
            map.put("assignmentId", t.getAssignment() != null ? t.getAssignment().getId() : null);
            map.put("studentId", t.getStudent() != null ? t.getStudent().getId() : null);
            map.put("triggerReason", t.getTriggerReason() != null ? t.getTriggerReason().name() : null);
            map.put("triggeredAt", t.getTriggeredAt());
            map.put("status", t.getStatus());
            map.put("payloadJson", t.getPayloadJson());
            return map;
        }).toList();
    }

    @Transactional
    public void deleteScenario(String scenarioId) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        // ⭐ 이거 핵심
        classroomRepository.clearActiveScenarioByScenarioId(scenarioId);

        scenarioRepository.delete(scenario);
    }

    public RandomQuizResponse getRandomQuiz(
            String scenarioId,
            String studentId,
            String assignmentId
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        List<ContentV4> quizzes = contentRepository.findByContentType(ContentType.QUIZ);

        if (quizzes == null || quizzes.isEmpty()) {
            throw new IllegalArgumentException("등록된 퀴즈 문제가 없습니다.");
        }

        /*
         * studentId / assignmentId 없이 호출하면 기존처럼 전체 퀴즈 중 랜덤 반환.
         * 단, 이 경우 중복 방지는 불가능함.
         */
        if (studentId == null || studentId.isBlank()
                || assignmentId == null || assignmentId.isBlank()) {

            ContentV4 quiz = quizzes.get(new Random().nextInt(quizzes.size()));

            return RandomQuizResponse.builder()
                    .available(true)
                    .contentId(quiz.getId())
                    .quizType(quiz.getQuizType() != null ? quiz.getQuizType().name() : null)
                    .question(quiz.getQuestion())
                    .option1(quiz.getOption1())
                    .option2(quiz.getOption2())
                    .option3(quiz.getOption3())
                    .option4(quiz.getOption4())
                    .submittedCount(null)
                    .remainingCount(null)
                    .totalQuizCount(null)
                    .message(null)
                    .build();
        }

        studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("assignment가 존재하지 않습니다."));

        if (assignment.getScenario() == null
                || !assignment.getScenario().getId().equals(scenario.getId())) {
            throw new IllegalArgumentException("해당 assignment는 이 시나리오 소속이 아닙니다.");
        }

        int totalQuizCount = getIntParam(assignment.getParamsJson(), "totalQuizCount", 5);

        List<String> submittedContentIds = quizSubmissionRepositoryV4.findSubmittedContentIds(
                scenarioId,
                assignmentId,
                studentId
        );

        int submittedCount = submittedContentIds != null ? submittedContentIds.size() : 0;

        if (submittedCount >= totalQuizCount) {
            return RandomQuizResponse.builder()
                    .available(false)
                    .submittedCount(submittedCount)
                    .remainingCount(0)
                    .totalQuizCount(totalQuizCount)
                    .message("랜덤 퀴즈 출제 횟수를 모두 사용했습니다.")
                    .build();
        }

        Set<String> submittedSet = new HashSet<>(submittedContentIds);

        List<ContentV4> availableQuizzes = quizzes.stream()
                .filter(q -> !submittedSet.contains(q.getId()))
                .toList();

        if (availableQuizzes.isEmpty()) {
            return RandomQuizResponse.builder()
                    .available(false)
                    .submittedCount(submittedCount)
                    .remainingCount(0)
                    .totalQuizCount(totalQuizCount)
                    .message("출제 가능한 퀴즈가 없습니다.")
                    .build();
        }

        ContentV4 quiz = availableQuizzes.get(new Random().nextInt(availableQuizzes.size()));

        return RandomQuizResponse.builder()
                .available(true)
                .contentId(quiz.getId())
                .quizType(quiz.getQuizType() != null ? quiz.getQuizType().name() : null)
                .question(quiz.getQuestion())
                .option1(quiz.getOption1())
                .option2(quiz.getOption2())
                .option3(quiz.getOption3())
                .option4(quiz.getOption4())
                .submittedCount(submittedCount)
                .remainingCount(totalQuizCount - submittedCount)
                .totalQuizCount(totalQuizCount)
                .message(null)
                .build();
    }

    public CallQuizResponse getCallQuiz(String scenarioId) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        List<CallQuizResponse.CallCard> cards = new ArrayList<>();

        cards.add(CallQuizResponse.CallCard.builder()
                .code("NAME")
                .label("신고자 이름")
                .build());

        cards.add(CallQuizResponse.CallCard.builder()
                .code("ADDRESS")
                .label("화재가 난 곳의 주소")
                .build());

        cards.add(CallQuizResponse.CallCard.builder()
                .code("PHONE")
                .label("신고자의 전화번호")
                .build());

        cards.add(CallQuizResponse.CallCard.builder()
                .code("CAUSE")
                .label("화재 원인")
                .build());

        // 프론트가 순서 맞추기 UI로 쓰기 쉽도록 카드 순서는 랜덤으로 내려줌
        Collections.shuffle(cards);

        return CallQuizResponse.builder()
                .scenarioId(scenario.getId())
                .missionCode("COMMON_REPORT_CALL")
                .cards(cards)
                .build();
    }

    @Transactional
    public CallSubmitResponse submitCallMission(
            String scenarioId,
            CallSubmitRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (req.getStudentId() == null || req.getStudentId().isBlank()) {
            throw new IllegalArgumentException("studentId는 필수입니다.");
        }

        if (req.getAssignmentId() == null || req.getAssignmentId().isBlank()) {
            throw new IllegalArgumentException("assignmentId는 필수입니다.");
        }

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(req.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("assignment가 존재하지 않습니다."));

        if (assignment.getScenario() == null ||
                !assignment.getScenario().getId().equals(scenario.getId())) {
            throw new IllegalArgumentException("해당 assignment는 이 시나리오 소속이 아닙니다.");
        }

        String missionCode = extractMissionCodeForScenarioService(
                assignment.getParamsJson(),
                assignment.getContent() != null ? assignment.getContent().getTitle() : null
        );

        if (!"COMMON_REPORT_CALL".equals(missionCode)) {
            throw new IllegalArgumentException("해당 assignment는 전화미션이 아닙니다.");
        }

        List<String> selectedOrder = normalizeCallOrder(req.getSelectedOrder());

        if (selectedOrder == null || selectedOrder.size() != 4) {
            throw new IllegalArgumentException("selectedOrder는 4개의 항목이어야 합니다.");
        }

        List<String> correctOrder = getCorrectCallOrder();

        boolean isCorrect = correctOrder.equals(selectedOrder);
        LocalDateTime now = LocalDateTime.now();

        // 전화미션 제출 로그 저장
        ScenarioActionEventV4 submitEvent = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.CALL_119_END)
                .valueText(isCorrect ? "CORRECT" : "FAILED")
                .metaJson(buildCallSubmitMetaJson(selectedOrder, correctOrder, assignment))
                .createdAt(now)
                .build();

        scenarioActionEventRepositoryV4.save(submitEvent);

        int requiredCount = 1;
        int progressCount = isCorrect ? 1 : 0;
        ProgressStatus progressStatus = isCorrect
                ? ProgressStatus.COMPLETED
                : ProgressStatus.IN_PROGRESS;

        upsertStudentMissionProgress(
                scenario,
                assignment,
                student,
                requiredCount,
                progressCount,
                progressStatus
        );

        if (isCorrect) {
            ScenarioActionEventV4 completeEvent = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .student(student)
                    .actionType(ScenarioActionType.MISSION_COMPLETE)
                    .valueText("COMMON_REPORT_CALL")
                    .createdAt(now)
                    .build();

            scenarioActionEventRepositoryV4.save(completeEvent);

            scenarioTriggerRepositoryV4
                    .findByScenario_IdAndStudent_IdAndAssignment_Id(
                            scenarioId,
                            student.getId(),
                            assignment.getId()
                    )
                    .ifPresent(trigger -> {
                        trigger.setStatus("COMPLETED");
                        scenarioTriggerRepositoryV4.save(trigger);
                    });
        }

        return CallSubmitResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .selectedOrder(selectedOrder)
                .correctOrder(correctOrder)
                .isCorrect(isCorrect)
                .missionCompleted(isCorrect)
                .requiredCount(requiredCount)
                .progressCount(progressCount)
                .status(progressStatus.name())
                .submittedAt(now)
                .build();
    }

    private List<String> getCorrectCallOrder() {
        return List.of("NAME", "ADDRESS", "PHONE", "CAUSE");
    }

    private List<String> normalizeCallOrder(List<String> selectedOrder) {
        if (selectedOrder == null) {
            return null;
        }

        return selectedOrder.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    private String buildCallSubmitMetaJson(
            List<String> selectedOrder,
            List<String> correctOrder,
            ScenarioAssignmentV4 assignment
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("missionCode", "COMMON_REPORT_CALL");
            map.put("assignmentId", assignment.getId());
            map.put("selectedOrder", selectedOrder);
            map.put("correctOrder", correctOrder);

            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String extractMissionCodeForScenarioService(String paramsJson, String title) {
        if (paramsJson != null && !paramsJson.isBlank()) {
            try {
                Map<?, ?> map = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(paramsJson, Map.class);

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

    public ExtinguisherQuizResponse getExtinguisherQuiz(String scenarioId) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        return ExtinguisherQuizResponse.builder()
                .scenarioId(scenario.getId())
                .missionCode("FIRETEAM_EXTINGUISHER_QUIZ")
                .life(3)
                .cooldownSeconds(10)

                // Unity가 카드 구성과 정답 판정을 담당한다는 의미
                .clientManaged(true)

                // 백엔드는 selectedOrder를 공식적으로 판정하지 않음
                .serverJudgement(false)

                .message("소화기 퀴즈 카드 구성과 정답 판정은 Unity에서 처리합니다. 백엔드는 /extinguisher-quiz/result API로 결과만 반영합니다.")

                // 기존 응답 호환용. 공식 흐름에서는 사용하지 않음.
                .cards(List.of())

                .build();
    }

    // Deprecated: selectedOrder 서버 판정 방식은 사용하지 않는다.
    // 공식 API는 submitExtinguisherQuizResult().
    @Transactional
    public ExtinguisherQuizSubmitResponse submitExtinguisherQuiz(
            String scenarioId,
            ExtinguisherQuizSubmitRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (req.getStudentId() == null || req.getStudentId().isBlank()) {
            throw new IllegalArgumentException("studentId는 필수입니다.");
        }

        if (req.getAssignmentId() == null || req.getAssignmentId().isBlank()) {
            throw new IllegalArgumentException("assignmentId는 필수입니다.");
        }

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(req.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("assignment가 존재하지 않습니다."));

        if (assignment.getScenario() == null ||
                !assignment.getScenario().getId().equals(scenario.getId())) {
            throw new IllegalArgumentException("해당 assignment는 이 시나리오 소속이 아닙니다.");
        }

        String missionCode = extractMissionCodeForScenarioService(
                assignment.getParamsJson(),
                assignment.getContent() != null ? assignment.getContent().getTitle() : null
        );

        if (!"FIRETEAM_EXTINGUISHER_QUIZ".equals(missionCode)) {
            throw new IllegalArgumentException("해당 assignment는 소화기 사용 퀴즈 미션이 아닙니다. missionCode=" + missionCode);
        }

        List<String> selectedOrder = normalizeExtinguisherOrder(req.getSelectedOrder());

        if (selectedOrder == null || selectedOrder.size() != 4) {
            throw new IllegalArgumentException("selectedOrder는 4개의 항목이어야 합니다.");
        }

        List<String> correctOrder = getCorrectExtinguisherOrder();

        boolean isCorrect = correctOrder.equals(selectedOrder);

        LocalDateTime now = LocalDateTime.now();

        int life = getIntParam(assignment.getParamsJson(), "life", 3);
        int cooldownSeconds = getIntParam(assignment.getParamsJson(), "cooldownSeconds", 10);

        int failedCountBefore = countExtinguisherQuizFailedAttempts(
                scenario.getId(),
                assignment.getId(),
                student.getId()
        );

        int failedCountAfter = isCorrect ? failedCountBefore : failedCountBefore + 1;

        int remainingLife = Math.max(life - failedCountAfter, 0);

        ScenarioActionEventV4 submitEvent = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.CARD_QUIZ_SUBMIT)
                .valueText(isCorrect ? "CORRECT" : "FAILED")
                .metaJson(buildExtinguisherQuizMetaJson(
                        assignment,
                        selectedOrder,
                        correctOrder,
                        life,
                        remainingLife,
                        cooldownSeconds
                ))
                .createdAt(now)
                .build();

        scenarioActionEventRepositoryV4.save(submitEvent);

        ProgressStatus progressStatus = isCorrect
                ? ProgressStatus.COMPLETED
                : ProgressStatus.IN_PROGRESS;

        int progressCount = isCorrect ? 1 : 0;

        upsertStudentMissionProgress(
                scenario,
                assignment,
                student,
                1,
                progressCount,
                progressStatus
        );

        if (isCorrect) {
            ScenarioActionEventV4 completeEvent = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .student(student)
                    .actionType(ScenarioActionType.MISSION_COMPLETE)
                    .valueText("FIRETEAM_EXTINGUISHER_QUIZ")
                    .createdAt(now)
                    .build();

            scenarioActionEventRepositoryV4.save(completeEvent);

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
        }

        return ExtinguisherQuizSubmitResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .selectedOrder(selectedOrder)
                .isCorrect(isCorrect)
                .missionCompleted(isCorrect)
                .life(life)
                .remainingLife(remainingLife)
                .cooldownSeconds(!isCorrect && remainingLife == 0 ? cooldownSeconds : 0)
                .requiredCount(1)
                .progressCount(progressCount)
                .status(progressStatus.name())
                .nextMissionCode(isCorrect ? "FIRETEAM_PUT_OUT_FIRE" : null)
                .submittedAt(now)
                .build();
    }

    // Deprecated: Unity가 로컬에서 퀴즈 정답을 판정하고,
    // 백엔드는 /extinguisher-quiz/result로 결과만 반영한다.
    // 이 메서드는 legacy submit fallback용으로만 유지한다.
    private List<String> getCorrectExtinguisherOrder() {
        return List.of(
                "GRAB_EXTINGUISHER",
                "PULL_PIN",
                "AIM_NOZZLE",
                "PRESS_HANDLE"
        );
    }

    private List<String> normalizeExtinguisherOrder(List<String> selectedOrder) {
        if (selectedOrder == null) {
            return null;
        }

        return selectedOrder.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .toList();
    }

    private int countExtinguisherQuizFailedAttempts(
            String scenarioId,
            String assignmentId,
            String studentId
    ) {
        return (int) scenarioActionEventRepositoryV4.findAll()
                .stream()
                .filter(event ->
                        event.getScenario() != null &&
                                scenarioId.equals(event.getScenario().getId()) &&
                                event.getStudent() != null &&
                                studentId.equals(event.getStudent().getId()) &&
                                event.getActionType() == ScenarioActionType.CARD_QUIZ_SUBMIT &&
                                "FAILED".equals(event.getValueText()) &&
                                event.getMetaJson() != null &&
                                event.getMetaJson().contains(assignmentId)
                )
                .count();
    }

    private String buildExtinguisherQuizMetaJson(
            ScenarioAssignmentV4 assignment,
            List<String> selectedOrder,
            List<String> correctOrder,
            int life,
            int remainingLife,
            int cooldownSeconds
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("missionCode", "FIRETEAM_EXTINGUISHER_QUIZ");
            map.put("assignmentId", assignment.getId());
            map.put("selectedOrder", selectedOrder);
            map.put("correctOrder", correctOrder);
            map.put("life", life);
            map.put("remainingLife", remainingLife);
            map.put("cooldownSeconds", cooldownSeconds);

            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Transactional
    public DonutGameProgressResponse progressDonutGame(
            String scenarioId,
            String assignmentId,
            DonutGameProgressRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (req.getStudentId() == null || req.getStudentId().isBlank()) {
            throw new IllegalArgumentException("studentId는 필수입니다.");
        }

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("assignment가 존재하지 않습니다."));

        if (assignment.getScenario() == null ||
                !assignment.getScenario().getId().equals(scenario.getId())) {
            throw new IllegalArgumentException("해당 assignment는 이 시나리오 소속이 아닙니다.");
        }

        String missionCode = extractMissionCodeForScenarioService(
                assignment.getParamsJson(),
                assignment.getContent() != null ? assignment.getContent().getTitle() : null
        );

        if (!"FIRETEAM_PUT_OUT_FIRE".equals(missionCode)) {
            throw new IllegalArgumentException("해당 assignment는 도넛 게임 미션이 아닙니다. missionCode=" + missionCode);
        }

        ScenarioTeamMemberV4 member = scenarioTeamMemberRepositoryV4
                .findByScenario_IdAndStudent_Id(scenario.getId(), student.getId())
                .orElseThrow(() -> new IllegalArgumentException("학생 팀 배정 정보가 없습니다."));

        ScenarioTeamV4 team = member.getTeam();

        if (team == null) {
            throw new IllegalArgumentException("학생이 팀에 배정되어 있지 않습니다.");
        }

        if (assignment.getTargetTeam() != null &&
                !assignment.getTargetTeam().getId().equals(team.getId())) {
            throw new IllegalArgumentException("해당 학생은 이 팀 미션 대상자가 아닙니다.");
        }

        ensureExtinguisherQuizCompleted(scenario, student);

        // 화재 발생 구역에 도착했는지 확인
        ensureStudentAtFireOrigin(scenario, student);

        FireteamStateResponse fireteamState = buildFireteamState(scenario, student);

        if (!"DONUT_ACTIVE".equals(fireteamState.getPhase())
                || !Boolean.TRUE.equals(fireteamState.getCanPlayDonut())) {

            throw new IllegalArgumentException(resolveDonutBlockedMessage(fireteamState));
        }

        int increment = req.getIncrementCount() == null || req.getIncrementCount() < 1
                ? 1
                : req.getIncrementCount();

        int requiredCount = getIntParam(
                assignment.getParamsJson(),
                "requiredClickCount",
                30
        );

        TeamMissionProgressV4 progress = teamMissionProgressRepositoryV4
                .findForUpdateByScenarioIdAndAssignmentIdAndTeamId(
                        scenario.getId(),
                        assignment.getId(),
                        team.getId()
                )
                .orElseGet(() -> TeamMissionProgressV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .assignment(assignment)
                        .team(team)
                        .requiredCount(requiredCount)
                        .progressCount(0)
                        .status(ProgressStatus.IN_PROGRESS)
                        .startedAt(LocalDateTime.now())
                        .build());

        if (progress.getStatus() == ProgressStatus.COMPLETED) {
            return DonutGameProgressResponse.builder()
                    .scenarioId(scenario.getId())
                    .assignmentId(assignment.getId())
                    .teamId(team.getId())
                    .teamCode(team.getTeamCode())
                    .studentId(student.getId())
                    .missionCode(missionCode)
                    .requiredCount(progress.getRequiredCount())
                    .progressCount(progress.getProgressCount())

                    // 요청은 들어왔지만 이미 완료 상태라 실제 반영은 0
                    .incrementCount(increment)
                    .acceptedIncrementCount(0)

                    .status(progress.getStatus().name())
                    .missionCompleted(true)

                    // 도넛 완료 후에는 분사 단계로 전환
                    .phase("SPRAY_READY")
                    .nextClientAction("START_SPRAY")
                    .waitRemainingSeconds(0)

                    .build();
        }

        int currentCount = progress.getProgressCount() != null ? progress.getProgressCount() : 0;
        int nextCount = Math.min(currentCount + increment, requiredCount);
        int acceptedIncrement = nextCount - currentCount;

        progress.setRequiredCount(requiredCount);
        progress.setProgressCount(nextCount);

        boolean completed = nextCount >= requiredCount;

        if (completed) {
            progress.setStatus(ProgressStatus.COMPLETED);
            progress.setCompletedAt(LocalDateTime.now());
        } else {
            progress.setStatus(ProgressStatus.IN_PROGRESS);
        }

        teamMissionProgressRepositoryV4.save(progress);

        ScenarioActionEventV4 incrementEvent = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.MISSION_INCREMENT)
                .valueInt(acceptedIncrement)
                .valueText("FIRETEAM_PUT_OUT_FIRE")
                .metaJson(buildDonutGameMetaJson(assignment, team, currentCount, nextCount, requiredCount))
                .createdAt(LocalDateTime.now())
                .build();

        scenarioActionEventRepositoryV4.save(incrementEvent);

        if (completed) {
            completeTeamMissionTriggers(scenario, assignment);

            ScenarioActionEventV4 completeEvent = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .student(student)
                    .actionType(ScenarioActionType.MISSION_COMPLETE)
                    .valueText("FIRETEAM_PUT_OUT_FIRE")
                    .metaJson(buildDonutGameMetaJson(assignment, team, currentCount, nextCount, requiredCount))
                    .createdAt(LocalDateTime.now())
                    .build();

            scenarioActionEventRepositoryV4.save(completeEvent);
        }

        return DonutGameProgressResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignment.getId())
                .teamId(team.getId())
                .teamCode(team.getTeamCode())
                .studentId(student.getId())
                .missionCode(missionCode)
                .requiredCount(requiredCount)
                .progressCount(nextCount)
                .incrementCount(increment)
                .acceptedIncrementCount(acceptedIncrement)
                .status(progress.getStatus().name())
                .missionCompleted(completed)
                .phase(completed ? "SPRAY_READY" : "DONUT_ACTIVE")
                .nextClientAction(completed ? "START_SPRAY" : "START_DONUT")
                .waitRemainingSeconds(0)
                .build();
    }

    private String resolveDonutBlockedMessage(FireteamStateResponse state) {
        if (state == null) {
            return "도넛 게임을 진행할 수 없습니다.";
        }

        if ("GET_EXTINGUISHER".equals(state.getPhase())) {
            return "소화기를 먼저 획득해야 합니다.";
        }

        if ("QUIZ".equals(state.getPhase())) {
            return "소화기 사용 퀴즈를 먼저 완료해야 합니다.";
        }

        if ("WAITING_TEAM_QUIZ".equals(state.getPhase())) {
            return "소화팀 전원이 소화기 사용 퀴즈를 완료해야 도넛 게임을 시작할 수 있습니다.";
        }

        if ("POST_QUIZ_DELAY".equals(state.getPhase())) {
            return "도넛 게임 시작 대기 중입니다. 남은 시간: " + state.getWaitRemainingSeconds() + "초";
        }

        if ("DONUT_ACTIVE".equals(state.getPhase())
                && !Boolean.TRUE.equals(state.getStudentAtFireOrigin())) {
            return "화재 발생 구역에 도착해야 도넛 게임을 진행할 수 있습니다.";
        }

        if ("SPRAY_READY".equals(state.getPhase())) {
            return "도넛 게임이 완료되었습니다. 소화기 분사 단계로 이동하세요.";
        }

        return "도넛 게임을 진행할 수 없습니다.";
    }

    private void ensureStudentAtFireOrigin(
            ScenarioV4 scenario,
            StudentV4 student
    ) {
        if (scenario.getDisasterOriginElementId() == null ||
                scenario.getDisasterOriginElementId().isBlank()) {
            throw new IllegalArgumentException("화재 발생 구역 정보가 없습니다.");
        }

        if (student.getLastBeacon() == null) {
            throw new IllegalArgumentException("학생의 현재 비콘 위치 정보가 없습니다.");
        }

        BeaconElementMapV4 mapping = beaconElementMapRepositoryV4
                .findByBeacon_IdAndActiveTrue(student.getLastBeacon().getId())
                .orElseThrow(() -> new IllegalArgumentException("현재 비콘의 활성 구조도 element 매핑이 없습니다."));

        String currentElementId = mapping.getEffectiveZoneElementId();

        if (currentElementId == null || currentElementId.isBlank()) {
            throw new IllegalArgumentException("현재 비콘의 zoneElementId 정보가 없습니다.");
        }

        if (!scenario.getDisasterOriginElementId().equals(currentElementId)) {
            throw new IllegalArgumentException("화재 발생 구역에 도착해야 도넛 게임을 진행할 수 있습니다.");
        }
    }

    private void ensureExtinguisherQuizCompleted(
            ScenarioV4 scenario,
            StudentV4 student
    ) {
        List<ScenarioAssignmentV4> assignments =
                scenarioAssignmentRepositoryV4.findByScenario_IdOrderByCreatedAtAsc(scenario.getId());

        ScenarioAssignmentV4 quizAssignment = assignments.stream()
                .filter(assignment -> "FIRETEAM_EXTINGUISHER_QUIZ".equals(
                        extractMissionCodeForScenarioService(
                                assignment.getParamsJson(),
                                assignment.getContent() != null ? assignment.getContent().getTitle() : null
                        )
                ))
                .findFirst()
                .orElse(null);

        if (quizAssignment == null) {
            throw new IllegalArgumentException("소화기 사용 퀴즈 assignment가 없습니다.");
        }

        boolean completed = studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenario.getId(),
                        quizAssignment.getId(),
                        student.getId()
                )
                .map(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .orElse(false);

        if (!completed) {
            throw new IllegalArgumentException("소화기 사용 퀴즈 완료 후 도넛 게임을 진행할 수 있습니다.");
        }
    }

    private void completeTeamMissionTriggers(
            ScenarioV4 scenario,
            ScenarioAssignmentV4 assignment
    ) {
        List<ScenarioTriggerV4> triggers =
                scenarioTriggerRepositoryV4.findByScenario_IdAndAssignment_Id(
                        scenario.getId(),
                        assignment.getId()
                );

        for (ScenarioTriggerV4 trigger : triggers) {
            trigger.setStatus("COMPLETED");
        }

        scenarioTriggerRepositoryV4.saveAll(triggers);
    }

    private String buildDonutGameMetaJson(
            ScenarioAssignmentV4 assignment,
            ScenarioTeamV4 team,
            int beforeCount,
            int afterCount,
            int requiredCount
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("missionCode", "FIRETEAM_PUT_OUT_FIRE");
            map.put("assignmentId", assignment.getId());
            map.put("teamId", team.getId());
            map.put("teamCode", team.getTeamCode());
            map.put("beforeCount", beforeCount);
            map.put("afterCount", afterCount);
            map.put("requiredCount", requiredCount);

            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Transactional(readOnly = true)
    public FireteamStateResponse getFireteamState(
            String scenarioId,
            String studentId
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        StudentV4 student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        return buildFireteamState(scenario, student);
    }

    private FireteamStateResponse buildFireteamState(
            ScenarioV4 scenario,
            StudentV4 student
    ) {
        ScenarioTeamMemberV4 myMember = scenarioTeamMemberRepositoryV4
                .findByScenario_IdAndStudent_Id(scenario.getId(), student.getId())
                .orElseThrow(() -> new IllegalArgumentException("학생 팀 배정 정보가 없습니다."));

        ScenarioTeamV4 fireTeam = myMember.getTeam();

        if (fireTeam == null || !"FIRE".equals(fireTeam.getTeamCode())) {
            throw new IllegalArgumentException("소화팀 학생만 조회할 수 있습니다.");
        }

        ScenarioAssignmentV4 getExtinguisherAssignment =
                findAssignmentByMissionCode(scenario.getId(), "FIRETEAM_GET_EXTINGUISHER");

        ScenarioAssignmentV4 quizAssignment =
                findAssignmentByMissionCode(scenario.getId(), "FIRETEAM_EXTINGUISHER_QUIZ");

        ScenarioAssignmentV4 donutAssignment =
                findAssignmentByMissionCode(scenario.getId(), "FIRETEAM_PUT_OUT_FIRE");

        List<ScenarioTeamMemberV4> fireMembers =
                scenarioTeamMemberRepositoryV4.findByScenario_IdAndTeam_IdOrderByAssignedAtAsc(
                        scenario.getId(),
                        fireTeam.getId()
                );

        List<FireteamStateResponse.MemberState> memberStates = new ArrayList<>();

        int quizCompletedCount = 0;
        LocalDateTime allQuizCompletedAt = null;

        for (ScenarioTeamMemberV4 member : fireMembers) {
            StudentV4 memberStudent = member.getStudent();

            StudentMissionProgressV4 quizProgress = studentMissionProgressRepository
                    .findByScenario_IdAndAssignment_IdAndStudent_Id(
                            scenario.getId(),
                            quizAssignment.getId(),
                            memberStudent.getId()
                    )
                    .orElse(null);

            String quizStatus = quizProgress != null && quizProgress.getStatus() != null
                    ? quizProgress.getStatus().name()
                    : "IN_PROGRESS";

            if ("COMPLETED".equals(quizStatus)) {
                quizCompletedCount++;

                LocalDateTime completedAt = quizProgress.getCompletedAt() != null
                        ? quizProgress.getCompletedAt()
                        : LocalDateTime.now();

                if (allQuizCompletedAt == null || completedAt.isAfter(allQuizCompletedAt)) {
                    allQuizCompletedAt = completedAt;
                }
            }

            memberStates.add(
                    FireteamStateResponse.MemberState.builder()
                            .studentId(memberStudent.getId())
                            .studentName(memberStudent.getStudentName())
                            .quizStatus(quizStatus)
                            .online(true)
                            .build()
            );
        }

        int requiredMemberCount = fireMembers.size();
        boolean allQuizCompleted = requiredMemberCount > 0 && quizCompletedCount == requiredMemberCount;

        String myQuizStatus = memberStates.stream()
                .filter(m -> student.getId().equals(m.getStudentId()))
                .map(FireteamStateResponse.MemberState::getQuizStatus)
                .findFirst()
                .orElse("IN_PROGRESS");

        boolean myExtinguisherCompleted = isStudentMissionCompleted(
                scenario.getId(),
                getExtinguisherAssignment.getId(),
                student.getId()
        );

        TeamMissionProgressV4 donutProgress = teamMissionProgressRepositoryV4
                .findByScenario_IdAndAssignment_IdAndTeam_Id(
                        scenario.getId(),
                        donutAssignment.getId(),
                        fireTeam.getId()
                )
                .orElse(null);

        int donutRequiredCount = getIntParam(donutAssignment.getParamsJson(), "requiredClickCount", 30);
        int donutProgressCount = donutProgress != null && donutProgress.getProgressCount() != null
                ? donutProgress.getProgressCount()
                : 0;

        String donutStatus = donutProgress != null && donutProgress.getStatus() != null
                ? donutProgress.getStatus().name()
                : "LOCKED";

        boolean donutCompleted = donutProgress != null && donutProgress.getStatus() == ProgressStatus.COMPLETED;

        int postQuizWaitSeconds = 5;
        int waitRemainingSeconds = 0;

        if (allQuizCompleted && allQuizCompletedAt != null) {
            long elapsed = java.time.Duration.between(allQuizCompletedAt, LocalDateTime.now()).getSeconds();
            waitRemainingSeconds = (int) Math.max(postQuizWaitSeconds - elapsed, 0);
        }

        String currentElementId = resolveCurrentElementId(student);
        String fireOriginElementId = scenario.getDisasterOriginElementId();

        boolean studentAtFireOrigin =
                fireOriginElementId != null &&
                        !fireOriginElementId.isBlank() &&
                        fireOriginElementId.equals(currentElementId);

        String phase;
        String nextClientAction;

        if (donutCompleted) {
            phase = "SPRAY_READY";
            nextClientAction = "START_SPRAY";
        } else if (!myExtinguisherCompleted) {
            phase = "GET_EXTINGUISHER";
            nextClientAction = "FIND_EXTINGUISHER";
        } else if (!"COMPLETED".equals(myQuizStatus)) {
            phase = "QUIZ";
            nextClientAction = "OPEN_QUIZ";
        } else if (!allQuizCompleted) {
            phase = "WAITING_TEAM_QUIZ";
            nextClientAction = "WAIT_TEAM_QUIZ";
        } else if (waitRemainingSeconds > 0) {
            phase = "POST_QUIZ_DELAY";
            nextClientAction = "WAIT_POST_QUIZ_DELAY";
        } else if (!studentAtFireOrigin) {
            phase = "DONUT_ACTIVE";
            nextClientAction = "MOVE_TO_FIRE_ZONE";
        } else {
            phase = "DONUT_ACTIVE";
            nextClientAction = "START_DONUT";
        }

        boolean canPlayDonut =
                "DONUT_ACTIVE".equals(phase) &&
                        "START_DONUT".equals(nextClientAction) &&
                        studentAtFireOrigin;

        String effectiveDonutStatus = donutStatus;

        if (donutCompleted) {
            effectiveDonutStatus = "COMPLETED";
        } else if ("DONUT_ACTIVE".equals(phase) && Boolean.TRUE.equals(canPlayDonut)) {
            effectiveDonutStatus = "IN_PROGRESS";
        }

        return FireteamStateResponse.builder()
                .scenarioId(scenario.getId())
                .teamId(fireTeam.getId())
                .teamCode(fireTeam.getTeamCode())
                .phase(phase)
                .nextClientAction(nextClientAction)
                .extinguisherAssignmentId(getExtinguisherAssignment.getId())
                .quizAssignmentId(quizAssignment.getId())
                .donutAssignmentId(donutAssignment.getId())
                .myQuizStatus(myQuizStatus)
                .allQuizCompleted(allQuizCompleted)
                .quizRequiredMemberCount(requiredMemberCount)
                .quizCompletedMemberCount(quizCompletedCount)
                .members(memberStates)
                .postQuizWaitSeconds(postQuizWaitSeconds)
                .waitRemainingSeconds(waitRemainingSeconds)
                .donutRequiredCount(donutRequiredCount)
                .donutProgressCount(donutProgressCount)
                .donutStatus(effectiveDonutStatus)
                .donutMissionCompleted(donutCompleted)
                .studentAtFireOrigin(studentAtFireOrigin)
                .fireOriginElementId(fireOriginElementId)
                .currentElementId(currentElementId)
                .canPlayDonut(canPlayDonut)
                .build();
    }

    private ScenarioAssignmentV4 findAssignmentByMissionCode(
            String scenarioId,
            String missionCode
    ) {
        return scenarioAssignmentRepositoryV4
                .findByScenario_IdOrderByCreatedAtAsc(scenarioId)
                .stream()
                .filter(assignment -> missionCode.equals(
                        extractMissionCodeForScenarioService(
                                assignment.getParamsJson(),
                                assignment.getContent() != null ? assignment.getContent().getTitle() : null
                        )
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("assignment 없음: " + missionCode));
    }

    private boolean isStudentMissionCompleted(
            String scenarioId,
            String assignmentId,
            String studentId
    ) {
        return studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenarioId,
                        assignmentId,
                        studentId
                )
                .map(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .orElse(false);
    }

    private String resolveCurrentElementId(StudentV4 student) {
        if (student.getLastBeacon() == null) {
            return null;
        }

        return beaconElementMapRepositoryV4
                .findByBeacon_IdAndActiveTrue(student.getLastBeacon().getId())
                .map(BeaconElementMapV4::getEffectiveZoneElementId)
                .orElse(null);
    }

    @Transactional
    public ExtinguisherQuizResultResponse submitExtinguisherQuizResult(
            String scenarioId,
            ExtinguisherQuizResultRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (req.getStudentId() == null || req.getStudentId().isBlank()) {
            throw new IllegalArgumentException("studentId는 필수입니다.");
        }

        if (req.getAssignmentId() == null || req.getAssignmentId().isBlank()) {
            throw new IllegalArgumentException("assignmentId는 필수입니다.");
        }

        if (req.getIsCorrect() == null) {
            throw new IllegalArgumentException("isCorrect는 필수입니다.");
        }

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(req.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("assignment가 존재하지 않습니다."));

        if (assignment.getScenario() == null ||
                !assignment.getScenario().getId().equals(scenario.getId())) {
            throw new IllegalArgumentException("해당 assignment는 이 시나리오 소속이 아닙니다.");
        }

        String missionCode = extractMissionCodeForScenarioService(
                assignment.getParamsJson(),
                assignment.getContent() != null ? assignment.getContent().getTitle() : null
        );

        if (!"FIRETEAM_EXTINGUISHER_QUIZ".equals(missionCode)) {
            throw new IllegalArgumentException("해당 assignment는 소화기 사용 퀴즈 미션이 아닙니다. missionCode=" + missionCode);
        }

        LocalDateTime now = LocalDateTime.now();

        boolean isCorrect = Boolean.TRUE.equals(req.getIsCorrect());

        boolean alreadyCompleted = studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenario.getId(),
                        assignment.getId(),
                        student.getId()
                )
                .map(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .orElse(false);

        ScenarioActionEventV4 submitEvent = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .student(student)
                .actionType(ScenarioActionType.CARD_QUIZ_SUBMIT)
                .valueText(isCorrect ? "CORRECT" : "FAILED")
                .metaJson(buildExtinguisherQuizResultMetaJson(
                        assignment,
                        isCorrect,
                        req.getRemainingLife(),
                        req.getAttemptCount()
                ))
                .createdAt(now)
                .build();

        scenarioActionEventRepositoryV4.save(submitEvent);

        int requiredCount = 1;
        int progressCount;
        ProgressStatus progressStatus;
        boolean missionCompleted;
        String nextMissionCode;

        if (isCorrect) {
            progressCount = 1;
            progressStatus = ProgressStatus.COMPLETED;
            missionCompleted = true;
            nextMissionCode = "FIRETEAM_PUT_OUT_FIRE";

            upsertStudentMissionProgress(
                    scenario,
                    assignment,
                    student,
                    requiredCount,
                    progressCount,
                    progressStatus
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

            if (!alreadyCompleted) {
                ScenarioActionEventV4 completeEvent = ScenarioActionEventV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .student(student)
                        .actionType(ScenarioActionType.MISSION_COMPLETE)
                        .valueText("FIRETEAM_EXTINGUISHER_QUIZ")
                        .createdAt(now)
                        .build();

                scenarioActionEventRepositoryV4.save(completeEvent);
            }

        } else {
            // 오답은 완료 처리하지 않음.
            // 이미 완료된 상태라면 완료 상태를 되돌리지 않음.
            if (alreadyCompleted) {
                progressCount = 1;
                progressStatus = ProgressStatus.COMPLETED;
                missionCompleted = true;
                nextMissionCode = "FIRETEAM_PUT_OUT_FIRE";
            } else {
                progressCount = 0;
                progressStatus = ProgressStatus.IN_PROGRESS;
                missionCompleted = false;
                nextMissionCode = null;

                upsertStudentMissionProgress(
                        scenario,
                        assignment,
                        student,
                        requiredCount,
                        progressCount,
                        progressStatus
                );
            }
        }

        return ExtinguisherQuizResultResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .missionCode("FIRETEAM_EXTINGUISHER_QUIZ")
                .isCorrect(isCorrect)
                .missionCompleted(missionCompleted)
                .remainingLife(req.getRemainingLife())
                .attemptCount(req.getAttemptCount())
                .requiredCount(requiredCount)
                .progressCount(progressCount)
                .status(progressStatus.name())
                .nextMissionCode(nextMissionCode)
                .submittedAt(now)
                .build();
    }

    private String buildExtinguisherQuizResultMetaJson(
            ScenarioAssignmentV4 assignment,
            boolean isCorrect,
            Integer remainingLife,
            Integer attemptCount
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source", "UNITY_LOCAL_RESULT");
            map.put("missionCode", "FIRETEAM_EXTINGUISHER_QUIZ");
            map.put("assignmentId", assignment.getId());
            map.put("isCorrect", isCorrect);
            map.put("remainingLife", remainingLife);
            map.put("attemptCount", attemptCount);

            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}