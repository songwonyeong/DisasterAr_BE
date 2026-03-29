package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.*;
import com.example.disaster_ar.domain.v4.enums.AcquiredSource;
import com.example.disaster_ar.dto.item.ItemAcquireRequest;
import com.example.disaster_ar.dto.item.StudentItemResponse;
import com.example.disaster_ar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}