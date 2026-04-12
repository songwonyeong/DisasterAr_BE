package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMapVersionFromChannelSetRequest {
    private String schoolId;
    private String label;
    private String createdByUserId;
}