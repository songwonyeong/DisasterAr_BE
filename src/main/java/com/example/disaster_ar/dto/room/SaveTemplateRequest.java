package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveTemplateRequest {
    private String templateName;
    private String description;
    private String createdBy;
}