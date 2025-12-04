package com.example.disaster_ar.dto.room;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomCreateRequest {

    private String schoolId;   // 어떤 채널(학교)에 속한 방인지
    private String userId;     // 방을 만든 선생님 / 관리자 ID
    private String className;  // 방 이름 (예: "3-2 화재훈련방")
}
