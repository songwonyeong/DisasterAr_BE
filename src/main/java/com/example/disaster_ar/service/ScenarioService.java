package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.*;
import com.example.disaster_ar.dto.scenario.*;
import com.example.disaster_ar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

        if (assignment.getContent() == null) {
            throw new IllegalArgumentException("퀴즈 content가 연결되어 있지 않습니다.");
        }

        ContentV4 content = assignment.getContent();

        if (content.getAnswer() == null) {
            throw new IllegalArgumentException("객관식/OX 정답이 설정되어 있지 않습니다.");
        }

        Integer correctAnswer = content.getAnswer();
        Integer selectedAnswer = req.getSelectedAnswer();

        if (selectedAnswer == null) {
            throw new IllegalArgumentException("선택한 답안이 없습니다.");
        }

        boolean isCorrect = correctAnswer.equals(selectedAnswer);
        LocalDateTime now = LocalDateTime.now();

        QuizSubmissionV4 submission = quizSubmissionRepositoryV4
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenarioId,
                        assignmentId,
                        student.getId()
                )
                .orElseGet(() -> QuizSubmissionV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .assignment(assignment)
                        .student(student)
                        .build());

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
                .createdAt(now)
                .build();

        scenarioActionEventRepositoryV4.save(event);

        return QuizSubmitResponse.builder()
                .scenarioId(scenario.getId())
                .assignmentId(assignment.getId())
                .studentId(student.getId())
                .selectedAnswer(selectedAnswer)
                .correctAnswer(correctAnswer)
                .isCorrect(isCorrect)
                .status(submission.getStatus().name())
                .submittedAt(submission.getSubmittedAt())
                .build();
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

    public ScenarioEvaluateResponse evaluateScenario(String scenarioId) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        List<StudentV4> students = studentRepository.findByClassroom_IdOrderByJoinedAtAsc(
                scenario.getClassroom().getId()
        );

        List<ScenarioEvaluateResponse.StudentEvaluationItem> studentResults = new java.util.ArrayList<>();
        double totalScore = 0.0;
        int counted = 0;

        for (StudentV4 student : students) {
            double score = 0.0;

            long completedMissionCount = studentMissionProgressRepository
                    .findAll()
                    .stream()
                    .filter(p ->
                            p.getScenario().getId().equals(scenarioId) &&
                                    p.getStudent().getId().equals(student.getId()) &&
                                    p.getStatus() == ProgressStatus.COMPLETED
                    )
                    .count();

            long correctQuizCount = quizSubmissionRepositoryV4
                    .findAll()
                    .stream()
                    .filter(q ->
                            q.getScenario().getId().equals(scenarioId) &&
                                    q.getStudent().getId().equals(student.getId()) &&
                                    Boolean.TRUE.equals(q.getIsCorrect())
                    )
                    .count();

            long correctCardQuizCount = cardQuizSubmissionRepositoryV4
                    .findAll()
                    .stream()
                    .filter(q ->
                            q.getScenario().getId().equals(scenarioId) &&
                                    q.getStudent().getId().equals(student.getId()) &&
                                    Boolean.TRUE.equals(q.getIsCorrect())
                    )
                    .count();

            score += completedMissionCount * 50.0;
            score += correctQuizCount * 20.0;
            score += correctCardQuizCount * 30.0;

            if (Boolean.TRUE.equals(student.getIsKicked())) {
                score -= 20.0;
            }

            if (score < 0) {
                score = 0.0;
            }

            String feedback = "미션 완료 " + completedMissionCount +
                    "개, 퀴즈 정답 " + correctQuizCount +
                    "개, 카드퀴즈 정답 " + correctCardQuizCount + "개";

            EvaluationV4 studentEval = EvaluationV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .level(EvalLevel.STUDENT)
                    .student(student)
                    .scoreTotal(score)
                    .feedbackText(feedback)
                    .createdAt(LocalDateTime.now())
                    .build();

            evaluationRepositoryV4.save(studentEval);

            studentResults.add(
                    ScenarioEvaluateResponse.StudentEvaluationItem.builder()
                            .studentId(student.getId())
                            .scoreTotal(score)
                            .feedbackText(feedback)
                            .build()
            );

            totalScore += score;
            counted++;
        }

        double scenarioScore = counted > 0 ? totalScore / counted : 0.0;

        EvaluationV4 scenarioEval = EvaluationV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .level(EvalLevel.SCENARIO)
                .scoreTotal(scenarioScore)
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
}