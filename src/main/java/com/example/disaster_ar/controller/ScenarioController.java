package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.scenario.ScenarioCreateRequest;
import com.example.disaster_ar.dto.scenario.ScenarioResponse;
import com.example.disaster_ar.dto.scenario.ScenarioUpdateRequest;
import com.example.disaster_ar.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
