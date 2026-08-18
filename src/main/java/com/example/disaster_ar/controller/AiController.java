package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.ai.AiChatRequest;
import com.example.disaster_ar.dto.ai.AiChatResponse;
import com.example.disaster_ar.dto.ai.AiFeedbackRequest;
import com.example.disaster_ar.dto.ai.AiFeedbackResponse;
import com.example.disaster_ar.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.disaster_ar.dto.ai.AiFeedbackPayloadResponse;
import com.example.disaster_ar.service.AiPayloadService;
import com.example.disaster_ar.dto.ai.AiRouteRequest;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiPayloadService aiPayloadService;

    @PostMapping("/chat/ask")
    public ResponseEntity<AiChatResponse> ask(
            @RequestBody AiChatRequest request
    ) {
        return ResponseEntity.ok(aiService.ask(request));
    }

    @PostMapping("/feedback")
    public ResponseEntity<AiFeedbackResponse> feedback(
            @RequestBody AiFeedbackRequest request
    ) {
        return ResponseEntity.ok(aiService.feedback(request));
    }

    @GetMapping("/scenarios/{scenarioId}/students/{studentId}/feedback-payload")
    public ResponseEntity<AiFeedbackPayloadResponse> getFeedbackPayload(
            @PathVariable String scenarioId,
            @PathVariable String studentId
    ) {
        return ResponseEntity.ok(
                aiPayloadService.buildFeedbackPayload(scenarioId, studentId)
        );
    }

    @PostMapping("/scenarios/{scenarioId}/students/{studentId}/feedback")
    public ResponseEntity<AiFeedbackResponse> generateFeedback(
            @PathVariable String scenarioId,
            @PathVariable String studentId
    ) {
        AiFeedbackPayloadResponse payload =
                aiPayloadService.buildFeedbackPayload(scenarioId, studentId);

        return ResponseEntity.ok(
                aiService.feedbackFromPayload(payload)
        );
    }

    @PostMapping("/scenarios/{scenarioId}/students/{studentId}/route-payload")
    public ResponseEntity<Map<String, Object>> getRoutePayload(
            @PathVariable String scenarioId,
            @PathVariable String studentId,
            @RequestBody AiRouteRequest request
    ) {
        log.info("🔥 1. 앱 요청 받음 route-payload scenarioId={}, studentId={}", scenarioId, studentId);
        log.info("🔥 1-1. 앱 요청 body 확인 route-payload targetElementId={}, target={}, currentBeaconElementId={}, currentBeacon={}, targetNodeId={}",
                request != null ? request.getTargetElementId() : null,
                request != null ? request.getTarget() : null,
                request != null ? request.getCurrentBeaconElementId() : null,
                request != null ? request.getCurrentBeacon() : null,
                request != null ? request.getTargetNodeId() : null);

        Map<String, Object> payload = aiPayloadService.buildRoutePayload(scenarioId, studentId, request);

        log.info("🔥 4-0. route-payload 응답 직전 scenarioId={}, studentId={}, current_beacon={}, target={}, target_node_id={}, stair_positions_exists={}, payload_keys={}",
                scenarioId,
                studentId,
                payload.get("current_beacon"),
                payload.get("target"),
                payload.get("target_node_id"),
                payload.containsKey("stair_positions"),
                payload.keySet());

        return ResponseEntity.ok(payload);
    }

    @PostMapping("/scenarios/{scenarioId}/students/{studentId}/route")
    public ResponseEntity<JsonNode> route(
            @PathVariable String scenarioId,
            @PathVariable String studentId,
            @RequestBody AiRouteRequest request
    ) {
        log.info("🔥 1. 앱 요청 받음 route scenarioId={}, studentId={}", scenarioId, studentId);
        log.info("🔥 1-1. 앱 요청 body 확인 route targetElementId={}, target={}, currentBeaconElementId={}, currentBeacon={}, targetNodeId={}",
                request != null ? request.getTargetElementId() : null,
                request != null ? request.getTarget() : null,
                request != null ? request.getCurrentBeaconElementId() : null,
                request != null ? request.getCurrentBeacon() : null,
                request != null ? request.getTargetNodeId() : null);

        Map<String, Object> payload = aiPayloadService.buildRoutePayload(scenarioId, studentId, request);

        log.info("🔥 4. AI 서버 요청 직전 scenarioId={}, studentId={}, current_beacon={}, target={}, target_node_id={}, stair_positions_exists={}, payload_keys={}",
                scenarioId,
                studentId,
                payload.get("current_beacon"),
                payload.get("target"),
                payload.get("target_node_id"),
                payload.containsKey("stair_positions"),
                payload.keySet());

        JsonNode response = aiService.route(payload);

        log.info("🔥 5. AI 서버 응답 scenarioId={}, studentId={}, found={}, warning={}, start_element_id={}, goal_element_id={}, path_size={}, response={}",
                scenarioId,
                studentId,
                response != null && response.has("found") ? response.get("found") : null,
                response != null && response.has("warning") ? response.get("warning") : null,
                response != null && response.has("start_element_id") ? response.get("start_element_id") : null,
                response != null && response.has("goal_element_id") ? response.get("goal_element_id") : null,
                response != null && response.has("path") && response.get("path").isArray() ? response.get("path").size() : null,
                response);

        return ResponseEntity.ok(response);
    }
}