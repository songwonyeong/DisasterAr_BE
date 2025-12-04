package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomUpdateRequest {

    // 어떤 방을 수정할지
    private String classroomId;

    // 요청 보낸 유저 (권한 체크용)
    private String userId;

    // 수정할 필드들 (null이면 변경 안 함)
    private String className;
    private Integer studentCount;
}
