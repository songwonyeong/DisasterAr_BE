package com.example.disaster_ar.dto.scenario;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CallMissionJudgeRequest {
    private Boolean success;
    private String memo;
    private String teacherUserId;
}
