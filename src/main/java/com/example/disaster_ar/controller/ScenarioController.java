package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.scenario.*;
import com.example.disaster_ar.service.ScenarioService;
import com.example.disaster_ar.service.ScenarioTeamAssignmentService;
import com.example.disaster_ar.service.ScenarioTeamDistributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.disaster_ar.dto.scenario.TriggeredAssignmentResponse;
import com.example.disaster_ar.dto.scenario.SimulateBeaconDetectRequest;
import com.example.disaster_ar.dto.scenario.SimulateBeaconDetectResponse;
import com.example.disaster_ar.service.ScenarioAdminService;
import com.example.disaster_ar.dto.scenario.RandomQuizResponse;
import com.example.disaster_ar.dto.scenario.TeamDistributionRequest;
import com.example.disaster_ar.dto.scenario.TeamDistributionResponse;
import com.example.disaster_ar.dto.scenario.CallQuizResponse;
import com.example.disaster_ar.dto.scenario.CallSubmitRequest;
import com.example.disaster_ar.dto.scenario.CallSubmitResponse;

import java.util.List;
import java.util.Map;

@Tag(name = "Scenario")
@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final ScenarioAdminService scenarioAdminService;
    private final ScenarioTeamDistributionService scenarioTeamDistributionService;
    private final ScenarioTeamAssignmentService scenarioTeamAssignmentService;

    // ✅ 시나리오 생성
    @Operation(summary = "시나리오 생성")
    @PostMapping
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody ScenarioCreateRequest req) {
        // req 안에 classroomId, scenarioName, scenarioType 등 들어있음
        return ResponseEntity.ok(scenarioService.create(req));
    }

    // ✅ 시나리오 수정
    @Operation(summary = "시나리오 수정")
    @PutMapping
    public ResponseEntity<ScenarioResponse> update(@Valid @RequestBody ScenarioUpdateRequest req) {
        return ResponseEntity.ok(scenarioService.update(req));
    }

    // ✅ 시나리오 목록 조회 (방/교실별)
    @Operation(summary = "시나리오 목록 조회(방/교실별)")
    @GetMapping("/classroom/{classroomId}")
    public ResponseEntity<List<ScenarioResponse>> list(@PathVariable String classroomId) {
        return ResponseEntity.ok(scenarioService.listByClassroom(classroomId));
    }

    @PostMapping("/{scenarioId}/actions")
    public ResponseEntity<ScenarioActionEventResponse> createActionEvent(
            @PathVariable String scenarioId,
            @RequestBody ScenarioActionEventRequest req
    ) {
        return ResponseEntity.ok(scenarioService.createActionEvent(scenarioId, req));
    }

    @PostMapping("/{scenarioId}/call/start")
    public ResponseEntity<Void> startCall(
            @PathVariable String scenarioId,
            @RequestBody CallMissionRequest req
    ) {
        scenarioService.startCallMission(scenarioId, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{scenarioId}/call/end")
    public ResponseEntity<Void> endCall(
            @PathVariable String scenarioId,
            @RequestBody CallMissionRequest req
    ) {
        scenarioService.endCallMission(scenarioId, req);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[26.04.08] 비콘 감지 시뮬레이션(테스트/관리자용)")
    @PostMapping("/{scenarioId}/simulate-beacon-detect")
    public ResponseEntity<SimulateBeaconDetectResponse> simulateBeaconDetect(
            @PathVariable String scenarioId,
            @RequestBody SimulateBeaconDetectRequest req
    ) {
        return ResponseEntity.ok(
                scenarioAdminService.simulateBeaconDetect(scenarioId, req)
        );
    }

    @GetMapping("/{scenarioId}/students/{studentId}/triggered-assignments")
    public ResponseEntity<java.util.List<TriggeredAssignmentResponse>> getTriggeredAssignments(
            @PathVariable String scenarioId,
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(
                scenarioService.getTriggeredAssignments(scenarioId, studentId)
        );
    }

    @PostMapping("/{scenarioId}/missions/{assignmentId}/complete")
    public ResponseEntity<MissionCompleteResponse> completeMission(
            @PathVariable String scenarioId,
            @PathVariable String assignmentId,
            @RequestBody MissionCompleteRequest req
    ) {
        return ResponseEntity.ok(
                scenarioService.completeMission(scenarioId, assignmentId, req)
        );
    }

    @PostMapping("/{scenarioId}/missions/{assignmentId}/progress")
    public ResponseEntity<MissionProgressResponse> progressMission(
            @PathVariable String scenarioId,
            @PathVariable String assignmentId,
            @RequestBody MissionProgressRequest req
    ) {
        return ResponseEntity.ok(
                scenarioService.progressMission(scenarioId, assignmentId, req)
        );
    }

    @PostMapping("/{scenarioId}/quizzes/{assignmentId}/submit")
    public ResponseEntity<QuizSubmitResponse> submitQuiz(
            @PathVariable String scenarioId,
            @PathVariable String assignmentId,
            @RequestBody QuizSubmitRequest req
    ) {
        return ResponseEntity.ok(
                scenarioService.submitQuiz(scenarioId, assignmentId, req)
        );
    }

    @PostMapping("/{scenarioId}/card-quizzes/{assignmentId}/submit")
    public ResponseEntity<CardQuizSubmitResponse> submitCardQuiz(
            @PathVariable String scenarioId,
            @PathVariable String assignmentId,
            @RequestBody CardQuizSubmitRequest req
    ) {
        return ResponseEntity.ok(
                scenarioService.submitCardQuiz(scenarioId, assignmentId, req)
        );
    }

    @PostMapping("/{scenarioId}/evaluate")
    public ResponseEntity<ScenarioEvaluateResponse> evaluateScenario(
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(scenarioService.evaluateScenario(scenarioId));
    }

    @GetMapping("/{scenarioId}/evaluations")
    public ResponseEntity<ScenarioEvaluationDetailResponse> getEvaluations(
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(
                scenarioService.getEvaluations(scenarioId)
        );
    }

    @PostMapping("/{scenarioId}/team-missions/{assignmentId}/steps/{stepOrder}/complete")
    public ResponseEntity<Void> completeTeamStep(
            @PathVariable String scenarioId,
            @PathVariable String assignmentId,
            @PathVariable Integer stepOrder,
            @RequestBody TeamStepCompleteRequest req
    ) {
        scenarioService.completeTeamStep(scenarioId, assignmentId, stepOrder, req);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{scenarioId}/team-missions/{assignmentId}/steps")
    public ResponseEntity<TeamStepStatusResponse> getTeamSteps(
            @PathVariable String scenarioId,
            @PathVariable String assignmentId,
            @RequestParam String teamId
    ) {
        return ResponseEntity.ok(
                scenarioService.getTeamSteps(scenarioId, assignmentId, teamId)
        );
    }

    @Operation(summary = "[26.04.22] 시나리오 트리거 로그 조회 API")
    @GetMapping("/triggers")
    public ResponseEntity<List<Map<String, Object>>> getTriggers(
            @RequestParam String scenarioId,
            @RequestParam(required = false) String studentId
    ) {
        return ResponseEntity.ok(
                scenarioService.getScenarioTriggers(scenarioId, studentId)
        );
    }

    @Operation(summary = "[26.04.27] 시나리오 팀 정원 계산 및 저장 API")
    @PostMapping("/{scenarioId}/teams/distribute")
    public ResponseEntity<TeamDistributionResponse> distributeTeams(
            @PathVariable String scenarioId,
            @RequestBody(required = false) TeamDistributionRequest req
    ) {
        return ResponseEntity.ok(
                scenarioTeamDistributionService.distributeTeams(scenarioId, req)
        );
    }

    @Operation(summary = "[26.04.27] 시나리오 학생 랜덤 팀 배정 API")
    @PostMapping("/{scenarioId}/teams/assign-students")
    public ResponseEntity<TeamAssignmentResponse> assignStudentsToTeams(
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(
                scenarioTeamAssignmentService.assignStudents(scenarioId)
        );

    }

    @Operation(summary = "[26.04.28] 시나리오 삭제 API")
    @DeleteMapping("/{scenarioId}")
    public ResponseEntity<Void> deleteScenario(@PathVariable String scenarioId) {
        scenarioService.deleteScenario(scenarioId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "[26.05.06] 랜덤 퀴즈 문제 조회")
    @GetMapping("/{scenarioId}/random-quiz")
    public ResponseEntity<RandomQuizResponse> getRandomQuiz(
            @PathVariable String scenarioId,
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String assignmentId
    ) {
        return ResponseEntity.ok(
                scenarioService.getRandomQuiz(scenarioId, studentId, assignmentId)
        );
    }

    @Operation(summary = "[26.05.10] 전화미션 카드 조회")
    @GetMapping("/{scenarioId}/call/quiz")
    public ResponseEntity<CallQuizResponse> getCallQuiz(
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(
                scenarioService.getCallQuiz(scenarioId)
        );
    }

    @Operation(summary = "[26.05.10] 전화미션 순서 제출")
    @PostMapping("/{scenarioId}/call/submit")
    public ResponseEntity<CallSubmitResponse> submitCallMission(
            @PathVariable String scenarioId,
            @RequestBody CallSubmitRequest req
    ) {
        return ResponseEntity.ok(
                scenarioService.submitCallMission(scenarioId, req)
        );
    }

    @Operation(summary = "[26.05.13] 소화기 사용 퀴즈 카드 조회 - Unity 판정형")
    @GetMapping("/{scenarioId}/extinguisher-quiz")
    public ResponseEntity<ExtinguisherQuizResponse> getExtinguisherQuiz(
            @PathVariable String scenarioId
    ) {
        return ResponseEntity.ok(
                scenarioService.getExtinguisherQuiz(scenarioId)
        );
    }

    @Deprecated
    @Operation(summary = "[Deprecated] 소화기 사용 퀴즈 selectedOrder 제출 - /extinguisher-quiz/result 사용")
    @PostMapping("/{scenarioId}/extinguisher-quiz/submit")
    public ResponseEntity<?> submitExtinguisherQuiz(
            @PathVariable String scenarioId,
            @RequestBody ExtinguisherQuizSubmitRequest req
    ) {
        throw new IllegalArgumentException(
                "이 API는 더 이상 사용하지 않습니다. /api/scenarios/{scenarioId}/extinguisher-quiz/result 를 사용해주세요."
        );
    }

    @Operation(summary = "[26.05.13] 도넛 게임 진행률 증가")
    @PostMapping("/{scenarioId}/team-missions/{assignmentId}/donut/progress")
    public ResponseEntity<DonutGameProgressResponse> progressDonutGame(
            @PathVariable String scenarioId,
            @PathVariable String assignmentId,
            @RequestBody DonutGameProgressRequest req
    ) {
        return ResponseEntity.ok(
                scenarioService.progressDonutGame(scenarioId, assignmentId, req)
        );
    }

    @Operation(summary = "[26.05.15] 소화팀 팀 상태 조회")
    @GetMapping("/{scenarioId}/fireteam/state")
    public ResponseEntity<FireteamStateResponse> getFireteamState(
            @PathVariable String scenarioId,
            @RequestParam String studentId
    ) {
        return ResponseEntity.ok(
                scenarioService.getFireteamState(scenarioId, studentId)
        );
    }

    @Operation(summary = "[26.05.17] 소화기 사용 퀴즈 결과 반영")
    @PostMapping("/{scenarioId}/extinguisher-quiz/result")
    public ResponseEntity<ExtinguisherQuizResultResponse> submitExtinguisherQuizResult(
            @PathVariable String scenarioId,
            @RequestBody ExtinguisherQuizResultRequest req
    ) {
        return ResponseEntity.ok(
                scenarioService.submitExtinguisherQuizResult(scenarioId, req)
        );
    }
}
