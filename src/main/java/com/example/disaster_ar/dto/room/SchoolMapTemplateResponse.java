package com.example.disaster_ar.dto.room;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SchoolMapTemplateResponse {

    private String templateId;
    private String schoolId;
    private String templateName;
    private String description;
    private String parentTemplateId;
    private String createdByUserId;
    private String floorsJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}