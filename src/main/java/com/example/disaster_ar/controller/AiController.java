package com.example.disaster_ar.controller;

import com.example.disaster_ar.dto.ai.AiChatRequest;
import com.example.disaster_ar.dto.ai.AiChatResponse;
import com.example.disaster_ar.dto.ai.AiFeedbackRequest;
import com.example.disaster_ar.dto.ai.AiFeedbackResponse;
import com.example.disaster_ar.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.disaster_ar.dto.ai.AiFeedbackPayloadResponse;
import com.example.disaster_ar.service.AiPayloadService;
import com.example.disaster_ar.dto.ai.AiRouteRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

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
        return ResponseEntity.ok(
                aiPayloadService.buildRoutePayload(scenarioId, studentId, request)
        );
    }

    @PostMapping("/scenarios/{scenarioId}/students/{studentId}/route")
    public ResponseEntity<JsonNode> route(
            @PathVariable String scenarioId,
            @PathVariable String studentId,
            @RequestBody AiRouteRequest request
    ) {
        Map<String, Object> payload =
                aiPayloadService.buildRoutePayload(scenarioId, studentId, request);

        return ResponseEntity.ok(
                aiService.route(payload)
        );
    }
}