package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomResponse {

    private String classroomId;
    private String schoolId;

    private String className;
    private Integer studentCount;

    private String joinCode;   // 방 입장 코드
}
