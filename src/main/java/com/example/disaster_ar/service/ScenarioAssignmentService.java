package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.ActorType;
import com.example.disaster_ar.domain.v4.enums.AssignmentType;
import com.example.disaster_ar.domain.v4.enums.TargetType;
import com.example.disaster_ar.dto.scenario.ScenarioAssignmentCreateRequest;
import com.example.disaster_ar.dto.scenario.ScenarioAssignmentResponse;
import com.example.disaster_ar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScenarioAssignmentService {

    private final ScenarioRepository scenarioRepository;
    private final ClassroomRepository classroomRepository;
    private final ScenarioAssignmentRepositoryV4 scenarioAssignmentRepositoryV4;
    private final BeaconRepositoryV4 beaconRepositoryV4;
    private final ScenarioTeamRepositoryV4 scenarioTeamRepositoryV4;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final ChannelElementTagRepositoryV4 channelElementTagRepositoryV4;

    @Transactional
    public ScenarioAssignmentResponse create(ScenarioAssignmentCreateRequest req) {
        ScenarioV4 scenario = scenarioRepository.findById(req.getScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        ClassroomV4 classroom = classroomRepository.findById(req.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        if (scenario.getClassroom() == null || !scenario.getClassroom().getId().equals(classroom.getId())) {
            throw new IllegalArgumentException("시나리오와 교실이 서로 일치하지 않습니다.");
        }

        if (classroom.getSchool() == null) {
            throw new IllegalArgumentException("교실에 연결된 학교가 없습니다.");
        }

        if (req.getFloorIndex() == null) {
            throw new IllegalArgumentException("floorIndex는 필수입니다.");
        }

        if ((req.getElementId() == null || req.getElementId().isBlank())
                && (req.getBeaconId() == null || req.getBeaconId().isBlank())) {
            throw new IllegalArgumentException("elementId 또는 beaconId 중 하나는 반드시 입력해야 합니다.");
        }

        ContentV4 content = contentRepository.findById(req.getContentId())
                .orElseThrow(() -> new IllegalArgumentException("콘텐츠가 존재하지 않습니다."));

        BeaconV4 beacon = null;
        if (req.getBeaconId() != null && !req.getBeaconId().isBlank()) {
            beacon = beaconRepositoryV4.findById(req.getBeaconId())
                    .orElseThrow(() -> new IllegalArgumentException("비콘이 존재하지 않습니다."));

            validateBeacon(classroom, req.getFloorIndex(), beacon);
        }

        ChannelElementTagV4 element = null;
        if (req.getElementId() != null && !req.getElementId().isBlank()) {
            element = channelElementTagRepositoryV4
                    .findBySchool_IdAndFloorIndexAndElementId(
                            classroom.getSchool().getId(),
                            req.getFloorIndex(),
                            req.getElementId()
                    )
                    .orElseThrow(() -> new IllegalArgumentException("해당 층에 element가 존재하지 않습니다."));
        }

        if (beacon != null && element != null) {
            validateBeaconAndElementSameScope(classroom, req.getFloorIndex(), beacon, element);
        }

        ScenarioTeamV4 targetTeam = null;
        if (req.getTargetTeamId() != null && !req.getTargetTeamId().isBlank()) {
            targetTeam = scenarioTeamRepositoryV4.findById(req.getTargetTeamId())
                    .orElseThrow(() -> new IllegalArgumentException("대상 팀이 존재하지 않습니다."));

            if (targetTeam.getScenario() == null || !targetTeam.getScenario().getId().equals(scenario.getId())) {
                throw new IllegalArgumentException("대상 팀이 해당 시나리오 소속이 아닙니다.");
            }
        }

        UserV4 createdByUser = null;
        if (req.getCreatedByUserId() != null && !req.getCreatedByUserId().isBlank()) {
            createdByUser = userRepository.findById(req.getCreatedByUserId())
                    .orElseThrow(() -> new IllegalArgumentException("생성자가 존재하지 않습니다."));
        }

        ScenarioAssignmentV4 assignment = ScenarioAssignmentV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .classroom(classroom)
                .assignmentType(AssignmentType.valueOf(req.getAssignmentType()))
                .content(content)
                .targetType(req.getTargetType() != null && !req.getTargetType().isBlank()
                        ? TargetType.valueOf(req.getTargetType())
                        : null)
                .targetTeam(targetTeam)
                .floorIndex(req.getFloorIndex())
                .elementId(req.getElementId())
                .beacon(beacon)
                .paramsJson(req.getParamsJson())
                .createdAt(LocalDateTime.now())
                .createdByType(req.getCreatedByType() != null && !req.getCreatedByType().isBlank()
                        ? ActorType.valueOf(req.getCreatedByType())
                        : null)
                .createdByUser(createdByUser)
                .build();

        ScenarioAssignmentV4 saved = scenarioAssignmentRepositoryV4.save(assignment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScenarioAssignmentResponse> getByScenario(String scenarioId) {
        return scenarioAssignmentRepositoryV4.findByScenario_IdOrderByCreatedAtAsc(scenarioId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(String assignmentId) {
        ScenarioAssignmentV4 assignment = scenarioAssignmentRepositoryV4.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("assignment가 존재하지 않습니다."));
        scenarioAssignmentRepositoryV4.delete(assignment);
    }

    private void validateBeacon(ClassroomV4 classroom, Integer floorIndex, BeaconV4 beacon) {
        if (beacon.getSchool() == null || !beacon.getSchool().getId().equals(classroom.getSchool().getId())) {
            throw new IllegalArgumentException("비콘이 해당 학교 소속이 아닙니다.");
        }

        if (!beacon.getFloorIndex().equals(floorIndex)) {
            throw new IllegalArgumentException("비콘 floorIndex와 assignment floorIndex가 일치하지 않습니다.");
        }
    }

    private void validateBeaconAndElementSameScope(
            ClassroomV4 classroom,
            Integer floorIndex,
            BeaconV4 beacon,
            ChannelElementTagV4 element
    ) {
        if (element.getSchool() == null || !element.getSchool().getId().equals(classroom.getSchool().getId())) {
            throw new IllegalArgumentException("element가 해당 학교 소속이 아닙니다.");
        }

        if (!element.getFloorIndex().equals(floorIndex)) {
            throw new IllegalArgumentException("element floorIndex와 assignment floorIndex가 일치하지 않습니다.");
        }

        if (!beacon.getFloorIndex().equals(element.getFloorIndex())) {
            throw new IllegalArgumentException("beacon과 element의 floorIndex가 서로 일치하지 않습니다.");
        }
    }

    private ScenarioAssignmentResponse toResponse(ScenarioAssignmentV4 a) {
        return ScenarioAssignmentResponse.builder()
                .id(a.getId())
                .scenarioId(a.getScenario().getId())
                .classroomId(a.getClassroom().getId())
                .assignmentType(a.getAssignmentType().name())
                .contentId(a.getContent().getId())
                .targetType(a.getTargetType() != null ? a.getTargetType().name() : null)
                .targetTeamId(a.getTargetTeam() != null ? a.getTargetTeam().getId() : null)
                .floorIndex(a.getFloorIndex())
                .elementId(a.getElementId())
                .beaconId(a.getBeacon() != null ? a.getBeacon().getId() : null)
                .paramsJson(a.getParamsJson())
                .createdByType(a.getCreatedByType() != null ? a.getCreatedByType().name() : null)
                .createdByUserId(a.getCreatedByUser() != null ? a.getCreatedByUser().getId() : null)
                .build();
    }
}