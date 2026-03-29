package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.scenario.*;
import com.example.disaster_ar.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.disaster_ar.dto.scenario.TriggeredAssignmentResponse;

import java.util.List;

@Tag(name = "Scenario")
@RestController
@RequestMapping("/api/scenarios")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

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

    @PostMapping("/{scenarioId}/beacon-detect")
    public ResponseEntity<BeaconDetectResponse> detectBeacon(
            @PathVariable String scenarioId,
            @RequestBody BeaconDetectRequest req
    ) {
        return ResponseEntity.ok(scenarioService.detectBeacon(scenarioId, req));
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
}
