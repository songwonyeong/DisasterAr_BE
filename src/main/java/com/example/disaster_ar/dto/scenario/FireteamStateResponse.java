package com.example.disaster_ar.dto.scenario;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FireteamStateResponse {

    private String scenarioId;
    private String teamId;
    private String teamCode;

    private String phase;
    private String nextClientAction;

    private String extinguisherAssignmentId;
    private String quizAssignmentId;
    private String donutAssignmentId;

    private String myQuizStatus;
    private Boolean allQuizCompleted;

    private Integer quizRequiredMemberCount;
    private Integer quizCompletedMemberCount;

    private List<MemberState> members;

    private Integer postQuizWaitSeconds;
    private Integer waitRemainingSeconds;

    private Integer donutRequiredCount;
    private Integer donutProgressCount;
    private String donutStatus;
    private Boolean donutMissionCompleted;

    private Boolean studentAtFireOrigin;
    private String fireOriginElementId;
    private String currentElementId;

    private Boolean canPlayDonut;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberState {
        private String studentId;
        private String studentName;
        private String quizStatus;
        private Boolean online;
    }
}