package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.QuizSubmissionV4;
import com.example.disaster_ar.domain.v4.ScenarioTeamMemberV4;
import com.example.disaster_ar.domain.v4.ScenarioTeamV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.dto.ai.AiFeedbackPayloadResponse;
import com.example.disaster_ar.dto.evaluation.StudentScoreBreakdown;
import com.example.disaster_ar.repository.QuizSubmissionRepositoryV4;
import com.example.disaster_ar.repository.ScenarioTeamMemberRepositoryV4;
import com.example.disaster_ar.repository.StudentRepositoryV4;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiPayloadService {

    private final StudentRepositoryV4 studentRepositoryV4;
    private final ScenarioTeamMemberRepositoryV4 scenarioTeamMemberRepositoryV4;
    private final QuizSubmissionRepositoryV4 quizSubmissionRepositoryV4;
    private final EvaluationScoreService evaluationScoreService;

    @Transactional(readOnly = true)
    public AiFeedbackPayloadResponse buildFeedbackPayload(
            String scenarioId,
            String studentId
    ) {
        StudentV4 student = studentRepositoryV4.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생이 존재하지 않습니다."));

        ScenarioTeamMemberV4 teamMember = scenarioTeamMemberRepositoryV4
                .findByScenario_IdAndStudent_Id(scenarioId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("학생의 팀 배정 정보가 없습니다."));

        ScenarioTeamV4 team = teamMember.getTeam();

        StudentScoreBreakdown breakdown =
                evaluationScoreService.calculateStudentScore(scenarioId, studentId);

        List<QuizSubmissionV4> quizSubmissions =
                quizSubmissionRepositoryV4.findByScenario_IdAndStudent_IdOrderBySubmittedAtAsc(
                        scenarioId,
                        studentId
                );

        return AiFeedbackPayloadResponse.builder()
                .studentId(student.getId())
                .studentName(student.getStudentName())
                .role(team != null ? normalizeRole(team.getTeamCode()) : null)
                .missions(List.of(
                        mission("EXTINGUISHER", breakdown.getExtinguisherFound()),
                        mission("SAFEZONE", breakdown.getSafeZoneCompleted()),
                        mission("FIRETEAM_EXTINGUISHER_ACQUIRED", breakdown.getFireteamExtinguisherAcquired()),
                        mission("FIRETEAM_EXTINGUISHER_QUIZ", breakdown.getFireteamExtinguisherQuizCompleted()),
                        mission("FIRETEAM_DONUT", breakdown.getFireteamDonutCompleted())
                ))
                .quizResults(
                        quizSubmissions.stream()
                                .map(q -> AiFeedbackPayloadResponse.QuizResult.builder()
                                        .quizId(resolveQuizId(q))
                                        .correct(Boolean.TRUE.equals(q.getIsCorrect()))
                                        .build())
                                .toList()
                )
                .reportCallCompleted(Boolean.TRUE.equals(breakdown.getReportCallCompleted()))
                .build();
    }

    private AiFeedbackPayloadResponse.MissionResult mission(
            String missionType,
            Boolean completed
    ) {
        return AiFeedbackPayloadResponse.MissionResult.builder()
                .missionType(missionType)
                .completed(Boolean.TRUE.equals(completed))
                .build();
    }

    private String resolveQuizId(QuizSubmissionV4 submission) {
        if (submission.getContent() != null) {
            return submission.getContent().getId();
        }

        if (submission.getAssignment() != null) {
            return submission.getAssignment().getId();
        }

        return submission.getId();
    }

    private String normalizeRole(String teamCode) {
        if (teamCode == null) {
            return null;
        }

        return switch (teamCode) {
            case "FIRE" -> "FIRETEAM";
            case "CIVILIAN" -> "CIVILIAN";
            case "FIRST_AID", "FIRSTAID" -> "FIRSTAID";
            default -> teamCode;
        };
    }
}