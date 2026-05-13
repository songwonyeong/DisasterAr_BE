package com.example.disaster_ar.dto.scenario;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonutGameProgressRequest {

    private String studentId;

    // 한 번 클릭할 때 증가량. null이면 1로 처리
    private Integer incrementCount;
}