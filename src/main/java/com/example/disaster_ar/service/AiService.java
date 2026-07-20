package com.example.disaster_ar.service;

import com.example.disaster_ar.dto.ai.AiChatRequest;
import com.example.disaster_ar.dto.ai.AiChatResponse;
import com.example.disaster_ar.dto.ai.AiFeedbackRequest;
import com.example.disaster_ar.dto.ai.AiFeedbackResponse;
import com.example.disaster_ar.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.example.disaster_ar.dto.ai.AiFeedbackPayloadResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient webClient;

    @Value("${ai.colab.base-url}")
    private String colabBaseUrl;

    @Value("${ai.colab.timeout-seconds:60}")
    private long timeoutSeconds;

    public AiChatResponse ask(AiChatRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw ApiException.badRequest(
                    "INVALID_AI_CHAT_REQUEST",
                    "질문 text는 필수입니다."
            );
        }

        Map<String, Object> body = Map.of(
                "text", request.getText().trim()
        );

        JsonNode response = postJson("/ask", body);

        return AiChatResponse.builder()
                .status(asText(response, "status"))
                .categoryUsed(asText(response, "category_used"))
                .answer(asText(response, "answer"))
                .context(asText(response, "context"))
                .build();
    }

    public AiFeedbackResponse feedback(AiFeedbackRequest request) {
        if (request == null) {
            throw ApiException.badRequest(
                    "INVALID_AI_FEEDBACK_REQUEST",
                    "피드백 요청값을 확인해 주세요."
            );
        }

        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            throw ApiException.badRequest(
                    "INVALID_AI_FEEDBACK_REQUEST",
                    "studentName은 필수입니다."
            );
        }

        Map<String, Object> body = toFeedbackServerBody(request);
        JsonNode response = postJson("/feedback", body);

        return AiFeedbackResponse.builder()
                .result(asText(response, "result"))
                .build();
    }

    public AiFeedbackResponse feedbackFromPayload(AiFeedbackPayloadResponse payload) {
        if (payload == null) {
            throw ApiException.badRequest(
                    "INVALID_AI_FEEDBACK_PAYLOAD",
                    "AI 피드백 payload가 비어 있습니다."
            );
        }

        if (payload.getStudentId() == null || payload.getStudentId().isBlank()) {
            throw ApiException.badRequest(
                    "INVALID_AI_FEEDBACK_PAYLOAD",
                    "studentId는 필수입니다."
            );
        }

        if (payload.getStudentName() == null || payload.getStudentName().isBlank()) {
            throw ApiException.badRequest(
                    "INVALID_AI_FEEDBACK_PAYLOAD",
                    "studentName은 필수입니다."
            );
        }

        Map<String, Object> body = toFeedbackServerBody(payload);
        JsonNode response = postJson("/feedback", body);

        return AiFeedbackResponse.builder()
                .result(resolveFeedbackResult(response))
                .build();
    }

    private Map<String, Object> toFeedbackServerBody(AiFeedbackPayloadResponse payload) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("student_name", payload.getStudentName().trim());

        List<Map<String, Object>> missions = new ArrayList<>();

        if (payload.getMissions() != null) {
            for (AiFeedbackPayloadResponse.MissionResult mission : payload.getMissions()) {
                if (mission == null) {
                    continue;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", toMissionTitle(mission.getMissionType()));
                item.put("status", Boolean.TRUE.equals(mission.getCompleted()) ? "완료" : "미완료");
                missions.add(item);
            }
        }

        body.put("missions", missions);

        // 현재 payload의 quizResults가 비어 있으므로 일단 빈 배열로 전달
        body.put("quizzes", new ArrayList<>());

        body.put("call_119", isMissionCompleted(payload, "CALL_119")
                || Boolean.TRUE.equals(payload.getReportCallCompleted()));

        return body;
    }

    private String toMissionTitle(String missionType) {
        if (missionType == null || missionType.isBlank()) {
            return "알 수 없는 미션";
        }

        return switch (missionType) {
            case "EXTINGUISHER_ACQUIRED" -> "소화기 찾기";
            case "CALL_119" -> "119 신고 순서 맞추기";
            case "SAFEZONE" -> "제한 시간 내 안전구역 도착";
            case "RANDOM_QUIZ" -> "랜덤 퀴즈 3개 이상 맞추기";

            case "FIRETEAM_DONUT" -> "소화팀: 도넛 게임으로 불 끄기";
            case "FIRETEAM_EXTINGUISHER_ACQUIRED" -> "소화팀: 소화기 획득";
            case "FIRETEAM_EXTINGUISHER_QUIZ" -> "소화팀: 소화기 사용 퀴즈";

            default -> missionType;
        };
    }

    private boolean isMissionCompleted(AiFeedbackPayloadResponse payload, String missionType) {
        if (payload == null || payload.getMissions() == null || missionType == null) {
            return false;
        }

        for (AiFeedbackPayloadResponse.MissionResult mission : payload.getMissions()) {
            if (mission == null) {
                continue;
            }

            if (missionType.equals(mission.getMissionType())
                    && Boolean.TRUE.equals(mission.getCompleted())) {
                return true;
            }
        }

        return false;
    }

    private String resolveFeedbackResult(JsonNode response) {
        String result = asText(response, "result");
        if (result != null) {
            return result;
        }

        String feedback = asText(response, "feedback");
        if (feedback != null) {
            return feedback;
        }

        String message = asText(response, "message");
        if (message != null) {
            return message;
        }

        return response != null ? response.toString() : null;
    }

    private JsonNode postJson(String path, Object body) {
        String url = normalizeBaseUrl(colabBaseUrl) + path;

        try {
            JsonNode response = webClient.post()
                    .uri(url)
                    .header("ngrok-skip-browser-warning", "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(Duration.ofSeconds(timeoutSeconds));

            if (response == null) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "AI_SERVER_EMPTY_RESPONSE",
                        "AI 서버 응답이 비어 있습니다. 호출 URL: " + url
                );
            }

            return response;

        } catch (ApiException e) {
            throw e;

        } catch (WebClientResponseException e) {
            String responseBody = e.getResponseBodyAsString(StandardCharsets.UTF_8);

            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_SERVER_HTTP_ERROR",
                    "AI 서버 HTTP 오류: "
                            + e.getStatusCode()
                            + " / URL: "
                            + url
                            + " / 응답: "
                            + responseBody
            );

        } catch (WebClientRequestException e) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_SERVER_CONNECTION_ERROR",
                    "AI 서버에 연결할 수 없습니다. URL: "
                            + url
                            + " / 원인: "
                            + e.getMessage()
            );

        } catch (Exception e) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_SERVER_ERROR",
                    "AI 서버 응답을 처리할 수 없습니다. URL: "
                            + url
                            + " / 원인: "
                            + e.getClass().getSimpleName()
                            + " - "
                            + e.getMessage()
            );
        }
    }

    private Map<String, Object> toFeedbackServerBody(AiFeedbackRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();

        body.put("student_name", request.getStudentName().trim());

        List<Map<String, Object>> missions = new ArrayList<>();
        if (request.getMissions() != null) {
            for (AiFeedbackRequest.MissionItem mission : request.getMissions()) {
                if (mission == null) {
                    continue;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", mission.getTitle());
                item.put("status", mission.getStatus());
                missions.add(item);
            }
        }
        body.put("missions", missions);

        List<Map<String, Object>> quizzes = new ArrayList<>();
        if (request.getQuizzes() != null) {
            for (AiFeedbackRequest.QuizItem quiz : request.getQuizzes()) {
                if (quiz == null) {
                    continue;
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("is_correct", Boolean.TRUE.equals(quiz.getIsCorrect()));
                quizzes.add(item);
            }
        }
        body.put("quizzes", quizzes);

        body.put("call_119", Boolean.TRUE.equals(request.getCall119()));

        return body;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "AI_SERVER_NOT_CONFIGURED",
                    "AI 서버 주소가 설정되어 있지 않습니다."
            );
        }

        String trimmed = value.trim();

        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    private String asText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return null;
        }

        JsonNode value = node.get(fieldName);

        if (value == null || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    public JsonNode route(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw ApiException.badRequest(
                    "INVALID_AI_ROUTE_PAYLOAD",
                    "AI 경로 탐색 payload가 비어 있습니다."
            );
        }

        return postJson("/route", payload);
    }
}