package com.example.disaster_ar.dto.channel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinClassroomResponse {

    private String classroomId;
    private String schoolId;

    private String className;
    private Integer studentCount;

    // 방 코드(join_code)
    private String joinCode;
}
