package com.example.disaster_ar.dto.channel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinSchoolRequest {

    // 프론트에서 '채널 코드' 입력
    private String channelCode;
}
