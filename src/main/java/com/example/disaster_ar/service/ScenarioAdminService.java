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
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScenarioAdminService {

    private final ScenarioRepository scenarioRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepositoryV4 studentRepository;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final StudentBeaconEventRepositoryV4 studentBeaconEventRepositoryV4;
    private final ScenarioTriggerService scenarioTriggerService;
    private final BeaconElementMapRepositoryV4 beaconElementMapRepositoryV4;

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

        if (classroom.getSchool() == null) {
            throw new IllegalArgumentException("교실에 학교 정보가 없습니다.");
        }

        StudentV4 student = studentRepository.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        if (student.getClassroom() == null || !student.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("학생이 해당 교실 소속이 아닙니다.");
        }

        BeaconV4 beacon = beaconRepositoryV4.findById(req.getBeaconId())
                .orElseThrow(() -> new IllegalArgumentException("비콘 없음"));

        /*
         * 시뮬레이션에서도 교실 학교와 비콘 학교가 같은지 확인한다.
         */
        if (beacon.getSchool() == null ||
                !classroom.getSchool().getId().equals(beacon.getSchool().getId())) {
            throw new IllegalArgumentException("교실의 학교와 비콘 학교가 일치하지 않습니다.");
        }

        Integer rssi = req.getRssi() != null ? req.getRssi() : -60;
        boolean updateLocation = Boolean.TRUE.equals(req.getUpdateLocation());
        boolean saveEvent = Boolean.TRUE.equals(req.getSaveEvent());

        LocalDateTime now = LocalDateTime.now();

        /*
         * 1차 변경 핵심:
         * 관리자 시뮬레이션도 active mapping이 있는 비콘만 인정한다.
         */
        BeaconElementMapV4 mapping = beaconElementMapRepositoryV4
                .findByBeacon_IdAndActiveTrue(beacon.getId())
                .orElse(null);

        if (mapping == null) {
            return SimulateBeaconDetectResponse.builder()
                    .scenarioId(scenario.getId())
                    .classroomId(classroom.getId())
                    .studentId(student.getId())
                    .beaconId(beacon.getId())
                    .locationUpdated(false)
                    .eventSaved(false)
                    .triggeredAssignmentIds(List.of())
                    .eventAt(now)
                    .message("활성 비콘 매핑이 없어 시뮬레이션을 무시했습니다.")
                    .build();
        }

        int thresholdRssi = mapping.getEffectiveThresholdRssi();

        /*
         * RSSI는 음수다.
         * 예:
         * -60 >= -85 통과
         * -90 >= -85 실패
         */
        if (rssi < thresholdRssi) {
            return SimulateBeaconDetectResponse.builder()
                    .scenarioId(scenario.getId())
                    .classroomId(classroom.getId())
                    .studentId(student.getId())
                    .beaconId(beacon.getId())
                    .locationUpdated(false)
                    .eventSaved(false)
                    .triggeredAssignmentIds(List.of())
                    .eventAt(now)
                    .message("RSSI가 thresholdRssi보다 약해서 시뮬레이션을 무시했습니다.")
                    .build();
        }

        BeaconV4 previousBeacon = student.getLastBeacon();

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
                    .fromBeacon(previousBeacon)
                    .toBeacon(beacon)
                    .rssi(rssi)
                    .eventAt(now)
                    .build();

            studentBeaconEventRepositoryV4.save(event);
        }

        /*
         * beacon 기준 trigger와 zoneElementId 기준 trigger를 둘 다 실행한다.
         * 중복 assignmentId는 Set으로 제거한다.
         */
        Set<String> triggeredIdSet = new LinkedHashSet<>();

        triggeredIdSet.addAll(
                scenarioTriggerService.triggerByBeacon(
                        scenario,
                        classroom,
                        student,
                        beacon,
                        rssi
                )
        );

        String zoneElementId = mapping.getEffectiveZoneElementId();

        if (zoneElementId != null && !zoneElementId.isBlank()) {
            triggeredIdSet.addAll(
                    scenarioTriggerService.triggerByElement(
                            scenario,
                            classroom,
                            student,
                            zoneElementId,
                            beacon,
                            rssi
                    )
            );
        }

        List<String> triggeredIds = new ArrayList<>(triggeredIdSet);

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