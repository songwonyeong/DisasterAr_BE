package com.example.disaster_ar.domain.enums;

public enum MonitoringStatus {
    ACTIVE,     // 학생이 이동 중
    SAFE,       // 대피 완료
    STUCK,      // 이동이 막힌 상태
    LOST        // 위치 신호 없음
}
