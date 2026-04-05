package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.BeaconState;
import com.example.disaster_ar.domain.v4.enums.ScenarioActionType;
import com.example.disaster_ar.domain.v4.enums.TriggerReason;
import com.example.disaster_ar.dto.beacon.BeaconScanRequest;
import com.example.disaster_ar.dto.beacon.BeaconSignal;
import com.example.disaster_ar.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BeaconTrackingService {

    private final StudentRepositoryV4 studentRepository;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final StudentBeaconEventRepositoryV4 studentBeaconEventRepositoryV4;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final ScenarioTriggerRepositoryV4 scenarioTriggerRepositoryV4;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;
    private final ObjectMapper objectMapper;

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
            handleScenarioTrigger(student, beacon, strongest.getRssi());
        }
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

    private void handleScenarioTrigger(
            StudentV4 student,
            BeaconV4 beacon,
            Integer rssi
    ) {
        ClassroomV4 classroom = student.getClassroom();
        if (classroom == null || classroom.getActiveScenario() == null) {
            return;
        }

        ScenarioV4 scenario = classroom.getActiveScenario();

        List<ScenarioAssignmentV4> assignments =
                scenarioAssignmentRepositoryV4.findByScenario_IdAndBeacon_Id(
                        scenario.getId(),
                        beacon.getId()
                );

        if (assignments.isEmpty()) {
            return;
        }

        for (ScenarioAssignmentV4 assignment : assignments) {
            Optional<ScenarioTriggerV4> existing =
                    scenarioTriggerRepositoryV4.findByScenario_IdAndStudent_IdAndAssignment_Id(
                            scenario.getId(),
                            student.getId(),
                            assignment.getId()
                    );

            if (existing.isPresent()) {
                continue;
            }

            String payloadJson = buildTriggerPayload(beacon, rssi);

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