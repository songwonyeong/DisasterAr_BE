package com.example.disaster_ar.controller;

import com.example.disaster_ar.domain.v4.SchoolMapTemplateV4;
import com.example.disaster_ar.dto.room.SaveTemplateRequest;
import com.example.disaster_ar.repository.SchoolMapTemplateRepositoryV4;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schools/{schoolId}/map-templates")
@RequiredArgsConstructor
public class SchoolMapTemplateController {

    private final SchoolMapTemplateRepositoryV4 schoolMapTemplateRepositoryV4;

    @GetMapping
    public ResponseEntity<List<SchoolMapTemplateV4>> getTemplates(
            @PathVariable String schoolId
    ) {
        return ResponseEntity.ok(
                schoolMapTemplateRepositoryV4.findBySchool_IdOrderByUpdatedAtDesc(schoolId)
        );
    }

    @GetMapping("/{templateId}")
    public ResponseEntity<SchoolMapTemplateV4> getTemplate(
            @PathVariable String schoolId,
            @PathVariable String templateId
    ) {
        SchoolMapTemplateV4 template = schoolMapTemplateRepositoryV4.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿이 존재하지 않습니다."));

        if (template.getSchool() == null || !template.getSchool().getId().equals(schoolId)) {
            throw new IllegalArgumentException("해당 학교의 템플릿이 아닙니다.");
        }

        return ResponseEntity.ok(template);
    }

    @PutMapping("/{templateId}")
    public ResponseEntity<SchoolMapTemplateV4> updateTemplate(
            @PathVariable String schoolId,
            @PathVariable String templateId,
            @RequestBody SaveTemplateRequest req
    ) {
        SchoolMapTemplateV4 template = schoolMapTemplateRepositoryV4.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));

        if (template.getSchool() == null || !template.getSchool().getId().equals(schoolId)) {
            throw new IllegalArgumentException("학교 불일치");
        }

        if (req.getTemplateName() != null) {
            template.setTemplateName(req.getTemplateName());
        }

        if (req.getDescription() != null) {
            template.setDescription(req.getDescription());
        }

        schoolMapTemplateRepositoryV4.save(template);

        return ResponseEntity.ok(template);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable String schoolId,
            @PathVariable String templateId
    ) {
        SchoolMapTemplateV4 template = schoolMapTemplateRepositoryV4.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("템플릿 없음"));

        if (template.getSchool() == null || !template.getSchool().getId().equals(schoolId)) {
            throw new IllegalArgumentException("학교 불일치");
        }

        schoolMapTemplateRepositoryV4.delete(template);

        return ResponseEntity.ok().build();
    }
}