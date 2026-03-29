package com.example.disaster_ar.dto.room;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveMapResponse {
    private String classroomId;
    private String activeMapVersionId;
    private String label;
}