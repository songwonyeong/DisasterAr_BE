package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.AcquiredSource;
import com.example.disaster_ar.dto.item.ItemAcquireRequest;
import com.example.disaster_ar.dto.item.StudentItemResponse;
import com.example.disaster_ar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.disaster_ar.domain.v4.enums.ProgressStatus;
import com.example.disaster_ar.domain.v4.enums.ScenarioActionType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ScenarioRepository scenarioRepository;
    private final StudentRepositoryV4 studentRepository;
    private final ItemRepositoryV4 itemRepositoryV4;
    private final StudentItemRepositoryV4 studentItemRepositoryV4;
    private final ScenarioActionEventRepositoryV4 scenarioActionEventRepositoryV4;
    private final ScenarioTeamMemberRepositoryV4 scenarioTeamMemberRepositoryV4;
    private final TeamMissionProgressRepositoryV4 teamMissionProgressRepositoryV4;

    // 1) 획득
    public StudentItemResponse acquireItem(
            String scenarioId,
            String studentId,
            ItemAcquireRequest req
    ) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        StudentV4 student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        ItemV4 item = itemRepositoryV4.findById(req.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("아이템 없음"));

        int qty = (req.getQuantity() == null || req.getQuantity() < 1) ? 1 : req.getQuantity();

        AcquiredSource source;
        try {
            source = req.getAcquiredSource() == null
                    ? AcquiredSource.SYSTEM
                    : AcquiredSource.valueOf(req.getAcquiredSource().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 acquiredSource");
        }

        StudentItemV4 si = studentItemRepositoryV4
                .findByScenario_IdAndStudent_IdAndItem_Id(scenarioId, studentId, item.getId())
                .orElseGet(() -> StudentItemV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .student(student)
                        .item(item)
                        .quantity(0)
                        .isConsumed(false)
                        .build());

        si.setQuantity(si.getQuantity() + qty);
        si.setAcquiredAt(LocalDateTime.now());
        si.setAcquiredSource(source);

        StudentItemV4 saved = studentItemRepositoryV4.save(si);

        recordPickupEvent(scenario, student, item, qty);
        incrementTeamMissionProgressOnPickup(scenario, student, item, qty);

        return toDto(saved);
    }

    // 2) 조회
    public List<StudentItemResponse> getStudentItems(String scenarioId, String studentId) {
        scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생 없음"));

        return studentItemRepositoryV4
                .findByScenario_IdAndStudent_Id(scenarioId, studentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 3) 사용/소모
    public StudentItemResponse consumeItem(String scenarioId, String studentId, String itemId) {
        StudentItemV4 si = studentItemRepositoryV4
                .findByScenario_IdAndStudent_IdAndItem_Id(scenarioId, studentId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이템 없음"));

        if (si.getQuantity() <= 0) {
            throw new IllegalArgumentException("사용할 수량 없음");
        }

        si.setQuantity(si.getQuantity() - 1);
        si.setIsConsumed(true);
        si.setConsumedAt(LocalDateTime.now());

        StudentItemV4 saved = studentItemRepositoryV4.save(si);
        return toDto(saved);
    }

    private StudentItemResponse toDto(StudentItemV4 si) {
        return StudentItemResponse.builder()
                .itemId(si.getItem().getId())
                .itemCode(si.getItem().getItemCode())
                .itemName(si.getItem().getItemName())
                .quantity(si.getQuantity())
                .isConsumed(si.getIsConsumed())
                .acquiredAt(si.getAcquiredAt())
                .consumedAt(si.getConsumedAt())
                .build();
    }

    private void recordPickupEvent(
            ScenarioV4 scenario,
            StudentV4 student,
            ItemV4 item,
            int qty
    ) {
        ScenarioActionEventV4 event = ScenarioActionEventV4.builder()
                .id(UUID.randomUUID().toString())
                .scenario(scenario)
                .classroom(student.getClassroom())
                .student(student)
                .actionType(ScenarioActionType.PICKUP_ITEM)
                .valueInt(qty)
                .valueText(item.getItemCode())
                .metaJson("{\"itemId\":\"" + item.getId() + "\",\"itemName\":\"" + item.getItemName() + "\"}")
                .createdAt(LocalDateTime.now())
                .build();

        scenarioActionEventRepositoryV4.save(event);
    }

    private void incrementTeamMissionProgressOnPickup(
            ScenarioV4 scenario,
            StudentV4 student,
            ItemV4 item,
            int qty
    ) {
        ScenarioTeamMemberV4 member = scenarioTeamMemberRepositoryV4
                .findByScenario_IdAndStudent_Id(scenario.getId(), student.getId())
                .orElse(null);

        if (member == null || member.getTeam() == null) {
            return;
        }

        List<TeamMissionProgressV4> progresses =
                teamMissionProgressRepositoryV4.findByScenario_IdAndTeam_IdAndStatus(
                        scenario.getId(),
                        member.getTeam().getId(),
                        ProgressStatus.IN_PROGRESS
                );

        if (progresses.isEmpty()) {
            return;
        }

        for (TeamMissionProgressV4 progress : progresses) {
            if (!isPickupRelatedMission(progress, item)) {
                continue;
            }

            int current = progress.getProgressCount() != null ? progress.getProgressCount() : 0;
            int required = progress.getRequiredCount() != null ? progress.getRequiredCount() : 1;

            int next = Math.min(required, current + qty);

            progress.setProgressCount(next);
            progress.setUpdateAt(LocalDateTime.now());

            if (next >= required) {
                progress.setStatus(ProgressStatus.COMPLETED);
                progress.setCompletedAt(LocalDateTime.now());
            }

            teamMissionProgressRepositoryV4.save(progress);
        }
    }

    private boolean isPickupRelatedMission(
            TeamMissionProgressV4 progress,
            ItemV4 item
    ) {
        if (progress.getAssignment() == null) {
            return false;
        }

        String itemCode = item.getItemCode() != null ? item.getItemCode().toUpperCase(Locale.ROOT) : "";
        String itemName = item.getItemName() != null ? item.getItemName() : "";
        String paramsJson = progress.getAssignment().getParamsJson();

        // 1. assignment.paramsJson에 itemCode/itemId가 있으면 가장 정확하게 매칭
        if (paramsJson != null) {
            if (paramsJson.contains(item.getId())) {
                return true;
            }
            if (!itemCode.isBlank() && paramsJson.toUpperCase(Locale.ROOT).contains(itemCode)) {
                return true;
            }
        }

        // 2. 임시 fallback: 소화기 계열 아이템이면 pickup 미션으로 판단
        return itemCode.contains("EXTINGUISH")
                || itemCode.contains("FIRE_EXT")
                || itemName.contains("소화기");
    }
}