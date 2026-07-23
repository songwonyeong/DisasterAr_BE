package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.QuizSubmissionV4;
import com.example.disaster_ar.domain.v4.ScenarioTeamMemberV4;
import com.example.disaster_ar.domain.v4.ScenarioTeamV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.dto.ai.AiFeedbackPayloadResponse;
import com.example.disaster_ar.dto.evaluation.StudentScoreBreakdown;
import com.example.disaster_ar.repository.QuizSubmissionRepositoryV4;
import com.example.disaster_ar.repository.ScenarioTeamMemberRepositoryV4;
import com.example.disaster_ar.repository.StudentRepositoryV4;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.dto.ai.AiRouteRequest;
import com.example.disaster_ar.repository.BeaconElementMapRepositoryV4;
import com.example.disaster_ar.repository.ScenarioRepository;
import com.example.disaster_ar.repository.ScenarioAssignmentRepositoryV4;
import com.example.disaster_ar.repository.StudentMissionProgressRepositoryV4;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AiPayloadService {

    private final StudentRepositoryV4 studentRepositoryV4;
    private final ScenarioTeamMemberRepositoryV4 scenarioTeamMemberRepositoryV4;
    private final QuizSubmissionRepositoryV4 quizSubmissionRepositoryV4;
    private final EvaluationScoreService evaluationScoreService;
    private final ScenarioRepository scenarioRepository;
    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;
    private final ObjectMapper objectMapper;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final StudentMissionProgressRepositoryV4 studentMissionProgressRepositoryV4;

    @Transactional(readOnly = true)
    public AiFeedbackPayloadResponse buildFeedbackPayload(
            String scenarioId,
            String studentId
    ) {
        StudentV4 student = studentRepositoryV4.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ScenarioTeamMemberV4 teamMember = scenarioTeamMemberRepositoryV4
                .findByScenario_IdAndStudent_Id(scenarioId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생의 팀 배정 정보가 없습니다."));

        ScenarioTeamV4 team = teamMember.getTeam();

        StudentScoreBreakdown breakdown =
                evaluationScoreService.calculateStudentScore(scenarioId, studentId);

        List<QuizSubmissionV4> quizSubmissions =
                quizSubmissionRepositoryV4.findByScenario_IdAndStudent_IdOrderBySubmittedAtAsc(
                        scenarioId,
                        studentId
                );

        String role = team != null ? normalizeRole(team.getTeamCode()) : null;

        List<ScenarioAssignmentV4> assignments =
                scenarioAssignmentRepositoryV4.findByScenario_IdOrderByCreatedAtAsc(scenarioId);

        List<StudentMissionProgressV4> progressRows =
                studentMissionProgressRepositoryV4.findByScenario_IdAndStudent_Id(
                        scenarioId,
                        studentId
                );

        return AiFeedbackPayloadResponse.builder()
                .studentId(student.getId())
                .studentName(student.getStudentName())
                .role(role)
                .missions(buildMissionResults(
                        team,
                        assignments,
                        progressRows,
                        breakdown
                ))
                .quizResults(
                        quizSubmissions.stream()
                                .map(q -> AiFeedbackPayloadResponse.QuizResult.builder()
                                        .quizId(resolveQuizId(q))
                                        .correct(Boolean.TRUE.equals(q.getIsCorrect()))
                                        .build())
                                .toList()
                )
                .reportCallCompleted(Boolean.TRUE.equals(breakdown.getReportCallCompleted()))
                .build();
    }

    private AiFeedbackPayloadResponse.MissionResult mission(
            String missionType,
            Boolean completed
    ) {
        return AiFeedbackPayloadResponse.MissionResult.builder()
                .missionType(missionType)
                .completed(Boolean.TRUE.equals(completed))
                .build();
    }

    private List<AiFeedbackPayloadResponse.MissionResult> buildMissionResults(
            ScenarioTeamV4 studentTeam,
            List<ScenarioAssignmentV4> assignments,
            List<StudentMissionProgressV4> progressRows,
            StudentScoreBreakdown breakdown
    ) {
        Map<String, StudentMissionProgressV4> progressByAssignmentId = new HashMap<>();

        for (StudentMissionProgressV4 progress : progressRows) {
            if (progress.getAssignment() != null) {
                progressByAssignmentId.put(progress.getAssignment().getId(), progress);
            }
        }

        Map<String, Boolean> missionCompletedByType = new LinkedHashMap<>();

        for (ScenarioAssignmentV4 assignment : assignments) {
            if (!isAssignmentTargetForStudent(assignment, studentTeam, progressByAssignmentId)) {
                continue;
            }

            String missionCode = extractMissionCode(assignment);
            String missionType = toAiMissionType(missionCode);

            if (missionType == null || missionType.isBlank()) {
                continue;
            }

            StudentMissionProgressV4 progress = progressByAssignmentId.get(assignment.getId());
            boolean completed = resolveMissionCompleted(missionType, progress, breakdown);

            missionCompletedByType.merge(missionType, completed, Boolean::logicalOr);
        }

        return missionCompletedByType.entrySet().stream()
                .map(entry -> mission(entry.getKey(), entry.getValue()))
                .toList();
    }

    private boolean isAssignmentTargetForStudent(
            ScenarioAssignmentV4 assignment,
            ScenarioTeamV4 studentTeam,
            Map<String, StudentMissionProgressV4> progressByAssignmentId
    ) {
        if (assignment == null) {
            return false;
        }

        // progress row가 있으면 이 학생에게 실제 생성된 미션으로 판단
        if (progressByAssignmentId.containsKey(assignment.getId())) {
            return true;
        }

        if (assignment.getTargetType() == null) {
            return true;
        }

        return switch (assignment.getTargetType()) {
            case ALL -> true;

            case TEAM -> studentTeam != null
                    && assignment.getTargetTeam() != null
                    && assignment.getTargetTeam().getId().equals(studentTeam.getId());

            case STUDENT -> false;
        };
    }

    private String extractMissionCode(ScenarioAssignmentV4 assignment) {
        if (assignment == null) {
            return null;
        }

        String paramsJson = assignment.getParamsJson();

        if (paramsJson != null && !paramsJson.isBlank()) {
            try {
                Map<?, ?> map = objectMapper.readValue(paramsJson, Map.class);
                Object missionCode = map.get("missionCode");

                if (missionCode != null) {
                    return String.valueOf(missionCode);
                }
            } catch (Exception ignored) {
            }
        }

        String title = assignment.getContent() != null
                ? assignment.getContent().getTitle()
                : null;

        if ("랜덤 퀴즈 3개 이상 맞추기".equals(title)) {
            return "COMMON_RANDOM_QUIZ";
        }

        if ("119 신고 순서 맞추기".equals(title) || "전화 미션".equals(title)) {
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

    private String toAiMissionType(String missionCode) {
        if (missionCode == null || missionCode.isBlank()) {
            return null;
        }

        return switch (missionCode) {
            case "COMMON_SAFE_ZONE" -> "SAFEZONE";
            case "COMMON_RANDOM_QUIZ" -> "RANDOM_QUIZ";
            case "COMMON_REPORT_CALL" -> "CALL_119";
            case "COMMON_FIND_EXTINGUISHER" -> "EXTINGUISHER_ACQUIRED";

            case "FIRETEAM_GET_EXTINGUISHER" -> "FIRETEAM_EXTINGUISHER_ACQUIRED";
            case "FIRETEAM_EXTINGUISHER_QUIZ" -> "FIRETEAM_EXTINGUISHER_QUIZ";
            case "FIRETEAM_PUT_OUT_FIRE" -> "FIRETEAM_DONUT";

            default -> missionCode;
        };
    }

    private boolean resolveMissionCompleted(
            String missionType,
            StudentMissionProgressV4 progress,
            StudentScoreBreakdown breakdown
    ) {
        boolean completedByProgress = progress != null
                && progress.getStatus() != null
                && "COMPLETED".equals(progress.getStatus().name());

        if (completedByProgress) {
            return true;
        }

        return switch (missionType) {
            case "SAFEZONE" -> Boolean.TRUE.equals(breakdown.getSafeZoneCompleted());
            case "RANDOM_QUIZ" -> Boolean.TRUE.equals(breakdown.getRandomQuizCompleted());
            case "CALL_119" -> Boolean.TRUE.equals(breakdown.getReportCallCompleted());
            case "EXTINGUISHER_ACQUIRED" -> Boolean.TRUE.equals(breakdown.getExtinguisherFound());

            case "FIRETEAM_EXTINGUISHER_ACQUIRED" ->
                    Boolean.TRUE.equals(breakdown.getFireteamExtinguisherAcquired());

            case "FIRETEAM_EXTINGUISHER_QUIZ" ->
                    Boolean.TRUE.equals(breakdown.getFireteamExtinguisherQuizCompleted());

            case "FIRETEAM_DONUT" ->
                    Boolean.TRUE.equals(breakdown.getFireteamDonutCompleted());

            default -> false;
        };
    }

    private String resolveQuizId(QuizSubmissionV4 submission) {
        if (submission.getContent() != null) {
            return submission.getContent().getId();
        }

        if (submission.getAssignment() != null) {
            return submission.getAssignment().getId();
        }

        return submission.getId();
    }

    private String normalizeRole(String teamCode) {
        if (teamCode == null) {
            return null;
        }

        return switch (teamCode) {
            case "FIRE" -> "FIRETEAM";
            case "CIVILIAN" -> "CIVILIAN";
            case "FIRST_AID", "FIRSTAID" -> "FIRSTAID";
            default -> teamCode;
        };
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildRoutePayload(
            String scenarioId,
            String studentId,
            AiRouteRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException("경로 탐색 요청 body는 필수입니다.");
        }

        String targetElementId = resolveRequestElementId(
                request.getTarget(),
                request.getTargetElementId()
        );

        if (targetElementId == null || targetElementId.isBlank()) {
            throw new IllegalArgumentException("target.element_id 또는 targetElementId는 필수입니다.");
        }

        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        StudentV4 student = studentRepositoryV4.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ClassroomV4 classroom = scenario.getClassroom();
        if (classroom == null) {
            throw new IllegalArgumentException("시나리오의 교실 정보가 없습니다.");
        }

        RoomMapVersionV4 mapVersion = scenario.getMapVersion() != null
                ? scenario.getMapVersion()
                : classroom.getActiveMapVersion();

        if (mapVersion == null) {
            throw new IllegalArgumentException("경로 탐색에 사용할 구조도가 없습니다.");
        }

        if (mapVersion.getFloorsJson() == null || mapVersion.getFloorsJson().isBlank()) {
            throw new IllegalArgumentException("구조도 floorsJson이 비어 있습니다.");
        }

        ParsedRouteMap parsedMap = parseRouteMap(mapVersion.getFloorsJson());

        String currentBeaconElementId = resolveRequestElementId(
                request.getCurrentBeacon(),
                request.getCurrentBeaconElementId()
        );

        if (currentBeaconElementId == null) {
            currentBeaconElementId = resolveCurrentBeaconElementId(student);
        }

        if (currentBeaconElementId == null) {
            throw new IllegalArgumentException("학생의 현재 비콘 위치가 없습니다.");
        }

        Integer currentBeaconFloor = resolveRequestFloor(
                request.getCurrentBeacon(),
                parsedMap.elementsJson(),
                currentBeaconElementId
        );
        Integer targetFloor = resolveRequestFloor(
                request.getTarget(),
                parsedMap.elementsJson(),
                targetElementId
        );

        List<String> disasterElementIds = new ArrayList<>();
        List<Map<String, Object>> disasterElements = new ArrayList<>();

        if (scenario.getDisasterOriginElementId() != null
                && !scenario.getDisasterOriginElementId().isBlank()) {
            String disasterElementId = scenario.getDisasterOriginElementId();
            disasterElementIds.add(disasterElementId);

            Integer disasterFloor = scenario.getDisasterOriginFloorIndex() != null
                    ? scenario.getDisasterOriginFloorIndex()
                    : findFloorByElementId(parsedMap.elementsJson(), disasterElementId);

            disasterElements.add(elementRef(disasterFloor, disasterElementId));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("elements_json", parsedMap.elementsJson());
        payload.put("tags_map", parsedMap.tagsMap());

        // 기존 AI 서버 하위 호환 필드
        payload.put("current_beacon_element_id", currentBeaconElementId);
        payload.put("target_element_id", targetElementId);
        payload.put("disaster_element_ids", disasterElementIds);

        // 신규 합의 필드: floor + element_id 조합으로 식별
        payload.put("current_beacon", elementRef(currentBeaconFloor, currentBeaconElementId));
        payload.put("target", elementRef(targetFloor, targetElementId));
        payload.put("disaster_elements", disasterElements);

        payload.put("stair_positions", parsedMap.stairPositions());
        payload.put("outline_bboxes", parsedMap.outlineBboxes());

        String resolvedTargetNodeId = request.getTargetNodeId();

        if (resolvedTargetNodeId == null || resolvedTargetNodeId.isBlank()) {
            resolvedTargetNodeId = targetElementId;
        }

        payload.put("target_node_id", resolvedTargetNodeId);

        return payload;
    }

    private String resolveRequestElementId(
            AiRouteRequest.ElementRef ref,
            String fallbackElementId
    ) {
        if (ref != null && ref.getElementId() != null && !ref.getElementId().isBlank()) {
            return ref.getElementId();
        }

        return asString(fallbackElementId);
    }

    private Integer resolveRequestFloor(
            AiRouteRequest.ElementRef ref,
            List<Map<String, Object>> elements,
            String elementId
    ) {
        if (ref != null && ref.getFloor() != null) {
            return ref.getFloor();
        }

        return findFloorByElementId(elements, elementId);
    }

    private Map<String, Object> elementRef(Integer floor, String elementId) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("floor", floor);
        ref.put("element_id", elementId);
        return ref;
    }

    private Integer findFloorByElementId(
            List<Map<String, Object>> elements,
            String elementId
    ) {
        if (elements == null || elementId == null || elementId.isBlank()) {
            return null;
        }

        for (Map<String, Object> element : elements) {
            String id = asString(element.get("id"));

            if (elementId.equals(id)) {
                return asInteger(element.get("floor"));
            }
        }

        return null;
    }

    private String resolveCurrentBeaconElementId(StudentV4 student) {
        if (student == null || student.getLastBeacon() == null) {
            return null;
        }

        String beaconId = student.getLastBeacon().getId();

        return beaconElementMapRepositoryV4
                .findByBeacon_IdAndActiveTrue(beaconId)
                .map(mapping -> {
                    if (mapping.getBeaconElementId() != null && !mapping.getBeaconElementId().isBlank()) {
                        return mapping.getBeaconElementId();
                    }

                    if (mapping.getEffectiveZoneElementId() != null
                            && !mapping.getEffectiveZoneElementId().isBlank()) {
                        return mapping.getEffectiveZoneElementId();
                    }

                    return beaconId;
                })
                .orElse(beaconId);
    }

    private record ParsedRouteMap(
            List<Map<String, Object>> elementsJson,
            Map<String, Object> tagsMap,
            List<Map<String, Object>> stairPositions,
            Map<String, Object> outlineBboxes
    ) {}

    @SuppressWarnings("unchecked")
    private ParsedRouteMap parseRouteMap(String floorsJson) {
        try {
            Object root = objectMapper.readValue(floorsJson, Object.class);

            List<?> floors;

            if (root instanceof Map<?, ?> rootMap && rootMap.get("floors") instanceof List<?> list) {
                floors = list;
            } else if (root instanceof List<?> list) {
                floors = list;
            } else {
                throw new IllegalArgumentException("floorsJson 형식이 올바르지 않습니다.");
            }

            List<Map<String, Object>> elementsJson = new ArrayList<>();
            Map<String, Object> tagsMap = new LinkedHashMap<>();
            List<Map<String, Object>> stairPositions = new ArrayList<>();
            Map<String, Object> outlineBboxes = new LinkedHashMap<>();

            for (Object floorObj : floors) {
                if (!(floorObj instanceof Map<?, ?> floor)) {
                    continue;
                }

                List<Map<String, Object>> elements = extractElements(floor);
                Integer floorIndex = resolveFloorIndex(floor, elements);

                if (floorIndex == null) {
                    floorIndex = 1;
                }

                Map<String, Object> outline = buildOutlineBbox(floor, elements);
                outlineBboxes.put(String.valueOf(floorIndex), outline);

                for (Map<String, Object> element : elements) {
                    String elementId = asString(firstNonNull(
                            element.get("id"),
                            element.get("elementId"),
                            element.get("element_id")
                    ));

                    if (elementId == null || elementId.isBlank()) {
                        continue;
                    }

                    Map<String, Object> copied = new LinkedHashMap<>(element);
                    copied.put("id", elementId);
                    copied.put("floor", floorIndex);

                    elementsJson.add(copied);

                    String zoneType = normalizeZoneType(resolveRawZoneType(element));

                    Map<String, Object> tagValue = new LinkedHashMap<>();
                    tagValue.put("zone_type", zoneType);
                    tagValue.put("passable", !"restricted".equalsIgnoreCase(zoneType));
                    tagValue.put("floor", floorIndex);
                    tagValue.put("element_id", elementId);

                    Object existingObj = tagsMap.get(elementId);

                    if (!(existingObj instanceof Map<?, ?> existingTag)) {
                        tagsMap.put(elementId, tagValue);
                    } else {
                        String existingZoneType = asString(existingTag.get("zone_type"));

                        if (existingZoneType == null || existingZoneType.isBlank()) {
                            existingZoneType = "normal";
                        }

                        boolean existingIsNormal = "normal".equalsIgnoreCase(existingZoneType);
                        boolean currentIsSpecial = zoneType != null
                                && !zoneType.isBlank()
                                && !"normal".equalsIgnoreCase(zoneType);

                        // 같은 elementId가 여러 층에 중복될 때,
                        // normal 값이 danger/safe 값을 덮어쓰지 못하게 막는다.
                        if (existingIsNormal && currentIsSpecial) {
                            tagsMap.put(elementId, tagValue);
                        }
                    }

                    if (isStairElement(element)) {
                        Double x = asDouble(element.get("x"));
                        Double y = asDouble(element.get("y"));
                        Double w = asDouble(element.get("width"));
                        Double h = asDouble(element.get("height"));

                        if (x != null && y != null) {
                            Map<String, Object> stair = new LinkedHashMap<>();
                            stair.put("x", x + (w != null ? w / 2.0 : 0));
                            stair.put("y", y + (h != null ? h / 2.0 : 0));
                            stair.put("floor", floorIndex);
                            stairPositions.add(stair);
                        }
                    }
                }
            }

            return new ParsedRouteMap(
                    elementsJson,
                    tagsMap,
                    stairPositions,
                    outlineBboxes
            );

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("경로 탐색용 구조도 JSON 파싱 실패", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractElements(Map<?, ?> floor) {
        Object elementsObj = firstNonNull(
                floor.get("elements"),
                floor.get("elementsJson"),
                floor.get("elements_json")
        );

        if (elementsObj instanceof String elementsString) {
            try {
                elementsObj = objectMapper.readValue(elementsString, Object.class);
            } catch (Exception e) {
                return List.of();
            }
        }

        if (!(elementsObj instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> elements = new ArrayList<>();

        for (Object obj : list) {
            if (obj instanceof Map<?, ?> map) {
                Map<String, Object> converted = new LinkedHashMap<>();

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    converted.put(String.valueOf(entry.getKey()), entry.getValue());
                }

                elements.add(converted);
            }
        }

        return elements;
    }

    private Integer resolveFloorIndex(
            Map<?, ?> floor,
            List<Map<String, Object>> elements
    ) {
        Integer floorIndex = firstInteger(
                floor.get("floorIndex"),
                floor.get("floor_index"),
                floor.get("floor"),
                floor.get("index"),
                floor.get("floorNo"),
                floor.get("floorNumber")
        );

        if (floorIndex != null) {
            return floorIndex;
        }

        if (elements != null) {
            for (Map<String, Object> element : elements) {
                floorIndex = firstInteger(
                        element.get("floorIndex"),
                        element.get("floor_index"),
                        element.get("floor"),
                        element.get("index"),
                        element.get("floorNo"),
                        element.get("floorNumber")
                );

                if (floorIndex != null) {
                    return floorIndex;
                }
            }
        }

        return null;
    }

    private String resolveRawZoneType(Map<String, Object> element) {
        return asString(firstNonNull(
                element.get("zoneType"),
                element.get("zone_type"),
                element.get("elementType"),
                element.get("element_type"),
                element.get("type"),
                element.get("label"),
                element.get("name")
        ));
    }

    private String normalizeZoneType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "normal";
        }

        String upper = raw.toUpperCase(Locale.ROOT);
        String compact = raw
                .replaceAll("\\s+", "")
                .replace("_", "")
                .replace("-", "")
                .trim();

        if (upper.contains("SAFE")
                || upper.contains("EVACUATION")
                || compact.contains("안전구역")
                || compact.contains("대피구역")
                || compact.contains("대피소")) {
            return "safe";
        }

        if (upper.contains("FIRE")
                || upper.contains("DANGER")
                || upper.contains("DISASTER")
                || compact.contains("화재")
                || compact.contains("위험")
                || compact.contains("재난")) {
            return "danger";
        }

        if (upper.contains("RESTRICTED")
                || upper.contains("BLOCK")
                || compact.contains("제한")
                || compact.contains("통제")) {
            return "restricted";
        }

        return "normal";
    }

    private boolean isStairElement(Map<String, Object> element) {
        if (element == null) {
            return false;
        }

        Object[] candidates = {
                element.get("type"),
                element.get("elementType"),
                element.get("element_type"),
                element.get("label"),
                element.get("name")
        };

        for (Object candidate : candidates) {
            String value = asString(candidate);

            if (value == null || value.isBlank()) {
                continue;
            }

            String upper = value.toUpperCase(Locale.ROOT);
            String compact = value
                    .replaceAll("\\s+", "")
                    .replace("_", "")
                    .replace("-", "")
                    .trim();

            if (upper.contains("STAIR")
                    || upper.contains("STAIRS")
                    || compact.contains("계단")) {
                return true;
            }
        }

        return false;
    }

    private Map<String, Object> buildOutlineBbox(
            Map<?, ?> floor,
            List<Map<String, Object>> elements
    ) {
        Double width = null;
        Double height = null;

        Object imageObj = firstNonNull(
                floor.get("image"),
                floor.get("imageUrl"),
                floor.get("uploadedImage"),
                floor.get("uploaded_image")
        );

        if (imageObj instanceof Map<?, ?> imageMap) {
            Object naturalObj = imageMap.get("natural");
            if (naturalObj instanceof Map<?, ?> naturalMap) {
                width = asDouble(firstNonNull(
                        naturalMap.get("w"),
                        naturalMap.get("width")
                ));
                height = asDouble(firstNonNull(
                        naturalMap.get("h"),
                        naturalMap.get("height")
                ));
            }

            if (width == null) {
                width = asDouble(firstNonNull(
                        imageMap.get("width"),
                        imageMap.get("naturalWidth"),
                        imageMap.get("natural_width")
                ));
            }

            if (height == null) {
                height = asDouble(firstNonNull(
                        imageMap.get("height"),
                        imageMap.get("naturalHeight"),
                        imageMap.get("natural_height")
                ));
            }
        }

        if (width == null || height == null) {
            double maxX = 0;
            double maxY = 0;

            if (elements != null) {
                for (Map<String, Object> element : elements) {
                    Double x = asDouble(element.get("x"));
                    Double y = asDouble(element.get("y"));
                    Double w = asDouble(element.get("width"));
                    Double h = asDouble(element.get("height"));

                    if (x != null) {
                        maxX = Math.max(maxX, x + (w != null ? w : 0));
                    }

                    if (y != null) {
                        maxY = Math.max(maxY, y + (h != null ? h : 0));
                    }
                }
            }

            width = width != null ? width : maxX;
            height = height != null ? height : maxY;
        }

        Map<String, Object> bbox = new LinkedHashMap<>();
        bbox.put("x0", 0);
        bbox.put("y0", 0);
        bbox.put("x1", width);
        bbox.put("y1", height);

        return bbox;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Integer firstInteger(Object... values) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            Integer parsed = asInteger(value);
            if (parsed != null) {
                return parsed;
            }
        }

        return null;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Double asDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(String.valueOf(value).trim());
        } catch (Exception e) {
            return null;
        }
    }
}