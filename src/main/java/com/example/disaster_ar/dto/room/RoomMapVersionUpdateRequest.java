package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomMapVersionUpdateRequest {
    private String label;
    private String floorsJson;
}