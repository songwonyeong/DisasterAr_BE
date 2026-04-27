package com.example.disaster_ar.controller;

import com.example.disaster_ar.domain.v4.SchoolMapTemplateV4;
import com.example.disaster_ar.dto.room.SaveTemplateRequest;
import com.example.disaster_ar.dto.room.SchoolMapTemplateResponse;
import com.example.disaster_ar.repository.SchoolMapTemplateRepositoryV4;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/map-templates")
@RequiredArgsConstructor
public class SchoolMapTemplateController {

    private final SchoolMapTemplateRepositoryV4 schoolMapTemplateRepositoryV4;

    @Operation(summary = "[26.04.27] 학교별 구조도 템플릿 목록 조회 API")
    @GetMapping
    public ResponseEntity<List<SchoolMapTemplateResponse>> getTemplates(
            @PathVariable String schoolId
    ) {
        List<SchoolMapTemplateResponse> responses =
                schoolMapTemplateRepositoryV4.findBySchool_IdOrderByUpdatedAtDesc(schoolId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "[26.04.27] 구조도 템플릿 단건 조회 API")
    @GetMapping("/{templateId}")
    public ResponseEntity<SchoolMapTemplateResponse> getTemplate(
            @PathVariable String schoolId,
            @PathVariable String templateId
    ) {
        SchoolMapTemplateV4 template = schoolMapTemplateRepositoryV4.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿이 존재하지 않습니다."));

        validateSchool(template, schoolId);

        return ResponseEntity.ok(toResponse(template));
    }

    @Operation(summary = "[26.04.27] 구조도 템플릿 수정 API")
    @PutMapping("/{templateId}")
    public ResponseEntity<SchoolMapTemplateResponse> updateTemplate(
            @PathVariable String schoolId,
            @PathVariable String templateId,
            @RequestBody SaveTemplateRequest req
    ) {
        SchoolMapTemplateV4 template = schoolMapTemplateRepositoryV4.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));

        validateSchool(template, schoolId);

        if (req.getTemplateName() != null) {
            template.setTemplateName(req.getTemplateName());
        }

        if (req.getDescription() != null) {
            template.setDescription(req.getDescription());
        }

        SchoolMapTemplateV4 saved = schoolMapTemplateRepositoryV4.save(template);

        return ResponseEntity.ok(toResponse(saved));
    }

    @Operation(summary = "[26.04.27] 구조도 템플릿 삭제 API")
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable String schoolId,
            @PathVariable String templateId
    ) {
        SchoolMapTemplateV4 template = schoolMapTemplateRepositoryV4.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));

        validateSchool(template, schoolId);

        schoolMapTemplateRepositoryV4.delete(template);

        return ResponseEntity.ok().build();
    }

    private void validateSchool(SchoolMapTemplateV4 template, String schoolId) {
        if (template.getSchool() == null || !template.getSchool().getId().equals(schoolId)) {
            throw new IllegalArgumentException("해당 학교의 템플릿이 아닙니다.");
        }
    }

    private SchoolMapTemplateResponse toResponse(SchoolMapTemplateV4 template) {
        return SchoolMapTemplateResponse.builder()
                .templateId(template.getId())
                .schoolId(template.getSchool() != null ? template.getSchool().getId() : null)
                .templateName(template.getTemplateName())
                .description(template.getDescription())
                .parentTemplateId(
                        template.getParentTemplate() != null
                                ? template.getParentTemplate().getId()
                                : null
                )
                .createdByUserId(
                        template.getCreatedBy() != null
                                ? template.getCreatedBy().getId()
                                : null
                )
                .floorsJson(template.getFloorsJson())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}