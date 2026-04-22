package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.scenario.ScenarioAssignmentCreateRequest;
import com.example.disaster_ar.dto.scenario.ScenarioAssignmentResponse;
import com.example.disaster_ar.service.ScenarioAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ScenarioAssignment")
@RestController
@RequestMapping("/api/scenario-assignments")
@RequiredArgsConstructor
public class ScenarioAssignmentController {

    private final ScenarioAssignmentService scenarioAssignmentService;

    @Operation(summary = "[26.04.22] 시나리오 assignment 생성 API")
    @PostMapping
    public ResponseEntity<ScenarioAssignmentResponse> create(
            @RequestBody ScenarioAssignmentCreateRequest request
    ) {
        return ResponseEntity.ok(
                scenarioAssignmentService.create(request)
        );
    }

    @Operation(summary = "[26.04.22] 시나리오 assignment 목록 조회 API")
    @GetMapping
    public ResponseEntity<List<ScenarioAssignmentResponse>> get(
            @RequestParam String scenarioId
    ) {
        return ResponseEntity.ok(
                scenarioAssignmentService.getByScenario(scenarioId)
        );
    }

    @Operation(summary = "[26.04.22] 시나리오 assignment 삭제 API")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        scenarioAssignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}