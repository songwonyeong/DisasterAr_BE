package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomMapVersionCreateRequest {
    private String schoolId;
    private String label;
    private String createdBy;
    private String floorsJson;
}