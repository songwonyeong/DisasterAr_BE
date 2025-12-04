package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScenarioCreateRequest {

    // 어떤 교실(Room)의 시나리오인지
    private String classroomId;

    // 시나리오 이름 (예: "3층 복도 화재 훈련 A")
    private String scenarioName;

    // 아래 4개는 문자열로 받고 서비스에서 enum으로 변환
    private String scenarioType;   // "FIRE", "EARTHQUAKE", "CHEMICAL", "OTHER"
    private String triggerMode;    // "AUTO", "MANUAL"
    private String teamMode;       // "AUTO", "MANUAL"
    private String npcMode;        // "AUTO", "MANUAL"

    private String location;       // 발생 위치 (ex. "3층 복도 끝")

    private Integer intensity;     // 강도
    private Integer trainTime;     // 훈련 시간(초 또는 분, 기준은 서버/기획에 맞게)

    // JSON 문자열 (프론트에서 그대로 보냄)
    private String teamAssignment; // 팀 배정 정보 JSON
    private String npcPositions;   // NPC 위치 정보 JSON

    private Integer participantCount; // 참가 인원 수
}
