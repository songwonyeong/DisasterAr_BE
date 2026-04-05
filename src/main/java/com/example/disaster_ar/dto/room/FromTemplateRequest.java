package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FromTemplateRequest {
    private String templateId;
    private String label;
    private String createdBy;
}