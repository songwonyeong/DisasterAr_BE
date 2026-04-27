package com.example.disaster_ar.dto.scenario;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TeamAssignmentResponse {

    private String scenarioId;
    private String classroomId;
    private Integer totalStudents;
    private List<TeamAssignmentResult> teams;

    @Getter
    @Builder
    public static class TeamAssignmentResult {
        private String teamId;
        private String teamCode;
        private String teamName;
        private Integer maxMembers;
        private Integer assignedCount;
        private List<StudentResult> students;
    }

    @Getter
    @Builder
    public static class StudentResult {
        private String studentId;
        private String studentName;
    }
}