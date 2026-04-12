package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.BeaconState;
import com.example.disaster_ar.dto.scenario.SimulateBeaconDetectRequest;
import com.example.disaster_ar.dto.scenario.SimulateBeaconDetectResponse;
import com.example.disaster_ar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScenarioAdminService {

    private final ScenarioRepository scenarioRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepositoryV4 studentRepository;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final StudentBeaconEventRepositoryV4 studentBeaconEventRepositoryV4;
    private final ScenarioTriggerService scenarioTriggerService;

    @Transactional
    public SimulateBeaconDetectResponse simulateBeaconDetect(
            String scenarioId,
            SimulateBeaconDetectRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        ClassroomV4 classroom = classroomRepository.findById(req.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("교실 없음"));

        if (!scenario.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("scenarioId와 classroomId가 일치하지 않습니다.");
        }

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        if (student.getClassroom() == null || !student.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("학생이 해당 교실 소속이 아닙니다.");
        }

        BeaconV4 beacon = beaconRepositoryV4.findById(req.getBeaconId())
                .orElseThrow(() -> new IllegalArgumentException("비콘 없음"));

        Integer rssi = req.getRssi() != null ? req.getRssi() : -60;
        boolean updateLocation = Boolean.TRUE.equals(req.getUpdateLocation());
        boolean saveEvent = Boolean.TRUE.equals(req.getSaveEvent());

        LocalDateTime now = LocalDateTime.now();
        BeaconV4 previousBeacon = student.getLastBeacon();   // 핵심: 먼저 저장

        if (updateLocation) {
            student.setLastBeacon(beacon);
            student.setLastBeaconRssi(rssi);
            student.setLastBeaconSeenAt(now);
            student.setBeaconState(BeaconState.DETECTED);
            studentRepository.save(student);
        }

        if (saveEvent) {
            StudentBeaconEventV4 event = StudentBeaconEventV4.builder()
                    .id(UUID.randomUUID().toString())
                    .scenario(scenario)
                    .student(student)
                    .fromBeacon(previousBeacon)   // 핵심: 이전 비콘 사용
                    .toBeacon(beacon)
                    .rssi(rssi)
                    .eventAt(now)
                    .build();

            studentBeaconEventRepositoryV4.save(event);
        }

        List<String> triggeredIds = scenarioTriggerService.triggerByBeacon(
                scenario,
                classroom,
                student,
                beacon,
                rssi
        );

        return SimulateBeaconDetectResponse.builder()
                .scenarioId(scenario.getId())
                .classroomId(classroom.getId())
                .studentId(student.getId())
                .beaconId(beacon.getId())
                .locationUpdated(updateLocation)
                .eventSaved(saveEvent)
                .triggeredAssignmentIds(triggeredIds)
                .eventAt(now)
                .message("Simulated beacon detection completed")
                .build();
    }
}