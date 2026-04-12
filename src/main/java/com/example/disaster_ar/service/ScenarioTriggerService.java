package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.ScenarioActionType;
import com.example.disaster_ar.domain.v4.enums.TriggerReason;
import com.example.disaster_ar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScenarioTriggerService {

    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final ScenarioTriggerRepositoryV4 scenarioTriggerRepositoryV4;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;
    private final ObjectMapper objectMapper;

    public List<String> triggerByBeacon(
            ScenarioV4 scenario,
            ClassroomV4 classroom,
            StudentV4 student,
            BeaconV4 beacon,
            Integer rssi
    ) {
        List<ScenarioAssignmentV4> assignments =
                scenarioAssignmentRepositoryV4.findByScenario_IdAndClassroom_IdAndBeacon_Id(
                        scenario.getId(),
                        classroom.getId(),
                        beacon.getId()
                );

        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> triggeredIds = new ArrayList<>();
        String payloadJson = buildTriggerPayload(beacon, rssi);

        for (ScenarioAssignmentV4 assignment : assignments) {
            boolean alreadyTriggered =
                    scenarioTriggerRepositoryV4.findByScenario_IdAndStudent_IdAndAssignment_Id(
                            scenario.getId(),
                            student.getId(),
                            assignment.getId()
                    ).isPresent();

            if (alreadyTriggered) {
                continue;
            }

            ScenarioTriggerV4 trigger = ScenarioTriggerV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .assignment(assignment)
                    .student(student)
                    .triggerReason(TriggerReason.BEACON_DETECTED)
                    .triggeredAt(LocalDateTime.now())
                    .status("TRIGGERED")
                    .payloadJson(payloadJson)
                    .build();

            scenarioTriggerRepositoryV4.save(trigger);
            triggeredIds.add(assignment.getId());

            ScenarioActionEventV4 actionEvent = ScenarioActionEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .classroom(classroom)
                    .student(student)
                    .actionType(ScenarioActionType.BEACON_ENTER)
                    .floorIndex(beacon.getFloorIndex())
                    .beacon(beacon)
                    .valueInt(rssi)
                    .valueText(beacon.getName())
                    .metaJson(payloadJson)
                    .createdAt(LocalDateTime.now())
                    .build();

            scenarioActionEventRepositoryV4.save(actionEvent);
        }

        return triggeredIds;
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