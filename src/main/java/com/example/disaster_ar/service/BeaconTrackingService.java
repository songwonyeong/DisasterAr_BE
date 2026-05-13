package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.BeaconState;
import com.example.disaster_ar.dto.beacon.BeaconScanRequest;
import com.example.disaster_ar.dto.beacon.BeaconSignal;
import com.example.disaster_ar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.disaster_ar.domain.v4.BeaconElementMapV4;
import com.example.disaster_ar.domain.v4.enums.ProgressStatus;
import com.example.disaster_ar.domain.v4.enums.ScenarioActionType;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BeaconTrackingService {

    private final StudentRepositoryV4 studentRepository;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final StudentBeaconEventRepositoryV4 studentBeaconEventRepositoryV4;
    private final ObjectMapper objectMapper;
    private final ScenarioTriggerService scenarioTriggerService;
    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final StudentMissionProgressRepositoryV4 studentMissionProgressRepository;
    private final ScenarioTriggerRepositoryV4 scenarioTriggerRepositoryV4;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;

    @Transactional
    public void processScan(BeaconScanRequest req) {
        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        if (student.getClassroom() == null || !student.getClassroom().getId().equals(req.getClassroomId())) {
            throw new IllegalArgumentException("학생이 해당 교실 소속이 아닙니다.");
        }

        if (req.getScans() == null || req.getScans().isEmpty()) {
            student.setBeaconState(BeaconState.LOST);
            studentRepository.save(student);
            return;
        }

        BeaconSignal strongest = req.getScans().stream()
                .filter(s -> s.getRssi() > -85)
                .max(Comparator.comparingInt(BeaconSignal::getRssi))
                .orElse(null);

        if (strongest == null) {
            student.setBeaconState(BeaconState.LOST);
            studentRepository.save(student);
            return;
        }

        BeaconV4 beacon = beaconRepositoryV4
                .findByUuidAndMajorAndMinor(
                        strongest.getUuid(),
                        strongest.getMajor(),
                        strongest.getMinor()
                )
                .orElse(null);

        if (beacon == null) {
            return;
        }

        if (student.getClassroom().getSchool() == null || beacon.getSchool() == null ||
                !student.getClassroom().getSchool().getId().equals(beacon.getSchool().getId())) {
            throw new IllegalArgumentException("학생 교실의 학교와 비콘 학교가 일치하지 않습니다.");
        }

        BeaconV4 previousBeacon = student.getLastBeacon();

        // 튐 방지: 이전 비콘이 있고 다른 비콘이면, 새 RSSI가 5 이상 강할 때만 변경
        if (previousBeacon != null && !previousBeacon.getId().equals(beacon.getId())) {
            int prevRssi = student.getLastBeaconRssi() != null ? student.getLastBeaconRssi() : -100;
            if (strongest.getRssi() < prevRssi + 5) {
                student.setLastBeaconSeenAt(LocalDateTime.now());
                student.setBeaconState(BeaconState.DETECTED);
                studentRepository.save(student);
                return;
            }
        }

        boolean changed = previousBeacon == null || !previousBeacon.getId().equals(beacon.getId());

        if (changed) {
            saveStudentBeaconEvent(student, previousBeacon, beacon, strongest.getRssi());
        }

        student.setLastBeacon(beacon);
        student.setLastBeaconRssi(strongest.getRssi());
        student.setLastBeaconSeenAt(LocalDateTime.now());
        student.setBeaconState(BeaconState.DETECTED);
        studentRepository.save(student);

        if (changed) {
            ClassroomV4 classroom = student.getClassroom();
            ScenarioV4 scenario = classroom != null ? classroom.getActiveScenario() : null;

            if (scenario != null) {
                scenarioTriggerService.triggerByBeacon(
                        scenario,
                        classroom,
                        student,
                        beacon,
                        strongest.getRssi()
                );
                Optional<BeaconElementMapV4> mapping =
                        beaconElementMapRepositoryV4.findByBeacon_Id(beacon.getId());

                if (mapping.isPresent()) {
                    String elementId = mapping.get().getElementId();

                    scenarioTriggerService.triggerByElement(
                            scenario,
                            classroom,
                            student,
                            elementId,
                            beacon,
                            strongest.getRssi()
                    );

                    completeSafeZoneMissionIfNeeded(
                            scenario,
                            classroom,
                            student,
                            elementId,
                            beacon,
                            strongest.getRssi()
                    );
                }
            }
        }
    }

    private void completeSafeZoneMissionIfNeeded(
            ScenarioV4 scenario,
            ClassroomV4 classroom,
            StudentV4 student,
            String elementId,
            BeaconV4 beacon,
            Integer rssi
    ) {
        if (scenario == null || classroom == null || student == null || elementId == null || elementId.isBlank()) {
            return;
        }

        if (!isSafeZoneElement(classroom, elementId)) {
            return;
        }

        List<ScenarioAssignmentV4> assignments =
                scenarioAssignmentRepositoryV4.findByScenario_IdOrderByCreatedAtAsc(scenario.getId());

        ScenarioAssignmentV4 safeZoneAssignment = assignments.stream()
                .filter(assignment -> "COMMON_SAFE_ZONE".equals(
                        extractMissionCode(
                                assignment.getParamsJson(),
                                assignment.getContent() != null ? assignment.getContent().getTitle() : null
                        )
                ))
                .findFirst()
                .orElse(null);

        if (safeZoneAssignment == null) {
            return;
        }

        boolean alreadyCompleted = studentMissionProgressRepository
                .findByScenario_IdAndAssignment_IdAndStudent_Id(
                        scenario.getId(),
                        safeZoneAssignment.getId(),
                        student.getId()
                )
                .map(progress -> progress.getStatus() == ProgressStatus.COMPLETED)
                .orElse(false);

        if (alreadyCompleted) {
            return;
        }

        upsertStudentMissionProgress(
                scenario,
                safeZoneAssignment,
                student,
                1,
                1,
                ProgressStatus.COMPLETED
        );

        scenarioTriggerRepositoryV4
                .findByScenario_IdAndStudent_IdAndAssignment_Id(
                        scenario.getId(),
                        student.getId(),
                        safeZoneAssignment.getId()
                )
                .ifPresent(trigger -> {
                    trigger.setStatus("COMPLETED");
                    scenarioTriggerRepositoryV4.save(trigger);
                });

        ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .classroom(classroom)
                .student(student)
                .actionType(ScenarioActionType.MISSION_COMPLETE)
                .floorIndex(beacon != null ? beacon.getFloorIndex() : null)
                .elementId(elementId)
                .beacon(beacon)
                .valueInt(rssi)
                .valueText("COMMON_SAFE_ZONE")
                .metaJson(buildSafeZoneMetaJson(safeZoneAssignment, elementId, beacon, rssi))
                .createdAt(LocalDateTime.now())
                .build();

        scenarioActionEventRepositoryV4.save(event);
    }

    private boolean isSafeZoneElement(ClassroomV4 classroom, String elementId) {
        if (classroom.getActiveMapVersion() == null
                || classroom.getActiveMapVersion().getFloorsJson() == null
                || classroom.getActiveMapVersion().getFloorsJson().isBlank()) {
            return false;
        }

        try {
            Object root = objectMapper.readValue(
                    classroom.getActiveMapVersion().getFloorsJson(),
                    Object.class
            );

            List<?> floors;

            if (root instanceof Map<?, ?> rootMap && rootMap.get("floors") instanceof List<?> list) {
                floors = list;
            } else if (root instanceof List<?> list) {
                floors = list;
            } else {
                return false;
            }

            for (Object floorObj : floors) {
                if (!(floorObj instanceof Map<?, ?> floor)) {
                    continue;
                }

                Object elementsObj = firstNonNull(
                        floor.get("elements"),
                        floor.get("elementsJson"),
                        floor.get("elements_json")
                );

                if (elementsObj instanceof String elementsString) {
                    elementsObj = objectMapper.readValue(elementsString, Object.class);
                }

                if (!(elementsObj instanceof List<?> elements)) {
                    continue;
                }

                for (Object elementObj : elements) {
                    if (!(elementObj instanceof Map<?, ?> element)) {
                        continue;
                    }

                    String currentElementId = asString(firstNonNull(
                            element.get("elementId"),
                            element.get("element_id"),
                            element.get("id")
                    ));

                    if (!elementId.equals(currentElementId)) {
                        continue;
                    }

                    return isSafeZoneElementMap(element);
                }
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSafeZoneElementMap(Map<?, ?> element) {
        String elementType = asString(firstNonNull(
                element.get("elementType"),
                element.get("element_type"),
                element.get("type")
        ));

        String name = asString(firstNonNull(
                element.get("name"),
                element.get("label"),
                element.get("elementName"),
                element.get("element_name")
        ));

        Object tagsObj = firstNonNull(
                element.get("tags"),
                element.get("tag"),
                element.get("tagsJson"),
                element.get("tags_json")
        );

        String merged = "";

        if (elementType != null) {
            merged += " " + elementType;
        }
        if (name != null) {
            merged += " " + name;
        }
        if (tagsObj != null) {
            merged += " " + tagsObj;
        }

        String upper = merged.toUpperCase(Locale.ROOT);

        return upper.contains("SAFE_ZONE")
                || upper.contains("SAFETY_ZONE")
                || upper.contains("EVACUATION_ZONE")
                || upper.contains("SAFE")
                || merged.contains("안전구역")
                || merged.contains("대피구역")
                || merged.contains("대피소");
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

    private String extractMissionCode(String paramsJson, String title) {
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

        if ("랜덤 퀴즈 3개 이상 맞추기".equals(title)) return "COMMON_RANDOM_QUIZ";
        if ("119 신고 순서 맞추기".equals(title)) return "COMMON_REPORT_CALL";
        if ("소화기 찾기".equals(title)) return "COMMON_FIND_EXTINGUISHER";
        if ("제한 시간 내 안전구역 도착".equals(title)) return "COMMON_SAFE_ZONE";
        if ("소화팀: 소화기 획득".equals(title)) return "FIRETEAM_GET_EXTINGUISHER";
        if ("소화팀: 소화기 사용 퀴즈".equals(title)) return "FIRETEAM_EXTINGUISHER_QUIZ";
        if ("소화팀: 도넛 게임으로 불 끄기".equals(title)) return "FIRETEAM_PUT_OUT_FIRE";

        return null;
    }

    private String buildSafeZoneMetaJson(
            ScenarioAssignmentV4 assignment,
            String elementId,
            BeaconV4 beacon,
            Integer rssi
    ) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source", "BEACON");
            map.put("missionCode", "COMMON_SAFE_ZONE");
            map.put("assignmentId", assignment.getId());
            map.put("elementId", elementId);
            map.put("beaconId", beacon != null ? beacon.getId() : null);
            map.put("rssi", rssi);

            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private void saveStudentBeaconEvent(
            StudentV4 student,
            BeaconV4 fromBeacon,
            BeaconV4 toBeacon,
            Integer rssi
    ) {
        ClassroomV4 classroom = student.getClassroom();
        ScenarioV4 activeScenario = classroom != null ? classroom.getActiveScenario() : null;

        if (activeScenario == null) {
            return;
        }

        StudentBeaconEventV4 event = StudentBeaconEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(activeScenario)
                .student(student)
                .fromBeacon(fromBeacon)
                .toBeacon(toBeacon)
                .rssi(rssi)
                .eventAt(LocalDateTime.now())
                .build();

        studentBeaconEventRepositoryV4.save(event);
    }

    private String buildTriggerPayload(BeaconV4 beacon, Integer rssi) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of(
                            "beaconId", beacon.getId(),
                            "floorIndex", beacon.getFloorIndex(),
                            "x", beacon.getX(),
                            "y", beacon.getY(),
                            "rssi", rssi
                    )
            );
        } catch (Exception e) {
            return "{}";
        }
    }
}