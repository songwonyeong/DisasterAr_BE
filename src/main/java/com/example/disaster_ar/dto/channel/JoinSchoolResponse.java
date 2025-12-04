package com.example.disaster_ar.dto.channel;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JoinSchoolResponse {

    private String schoolId;
    private String schoolName;

    // 채널 코드 = schools.access_code
    private String channelCode;

    // 이 학교(채널)에 연결된 지도 파일 경로들
    private List<String> mapFiles;
}
