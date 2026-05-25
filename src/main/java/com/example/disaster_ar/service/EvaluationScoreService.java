package com.example.disaster_ar.service;

import com.example.disaster_ar.dto.evaluation.StudentScoreBreakdown;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationScoreService {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public StudentScoreBreakdown calculateStudentScore(String scenarioId, String studentId) {
        int correctQuizCount = countCorrectQuizSubmissions(scenarioId, studentId);

        double quizScore = correctQuizCount * 6.0;

        boolean randomQuizCompleted = existsCompletedMissionCode(
                scenarioId,
                studentId,
                "COMMON_RANDOM_QUIZ"
        );

        /*
         * Python personal_evaluator.py는 "정답 퀴즈가 하나라도 있으면 랜덤 퀴즈 성공 +8"로 봄.
         * Spring에서는 missionCode가 있으면 COMMON_RANDOM_QUIZ 완료를 우선 보고,
         * 기존 데이터 호환을 위해 정답 퀴즈 존재도 fallback으로 인정한다.
         */
        boolean randomQuizSuccess = randomQuizCompleted || correctQuizCount > 0;

        boolean reportCallCompleted = existsCompletedMissionCode(
                scenarioId,
                studentId,
                "COMMON_REPORT_CALL"
        );

        boolean extinguisherFound =
                existsNotConsumedExtinguisherItem(scenarioId, studentId)
                        || existsCompletedMissionCode(
                        scenarioId,
                        studentId,
                        "COMMON_FIND_EXTINGUISHER"
                );

        double personalScore = 0.0;
        if (randomQuizSuccess) {
            personalScore += 8.0;
        }
        if (reportCallCompleted) {
            personalScore += 6.0;
        }
        if (extinguisherFound) {
            personalScore += 6.0;
        }

        boolean safeZoneCompleted = existsCompletedMissionCode(
                scenarioId,
                studentId,
                "COMMON_SAFE_ZONE"
        );

        double safezoneScore = safeZoneCompleted ? 10.0 : 0.0;

        boolean fireteamExtinguisherAcquired =
                existsCompletedMissionCode(
                        scenarioId,
                        studentId,
                        "FIRETEAM_GET_EXTINGUISHER"
                )
                        || existsPickupFireExtinguisherAction(
                        scenarioId,
                        studentId
                );

        boolean fireteamExtinguisherQuizCompleted =
                existsCompletedMissionCode(
                        scenarioId,
                        studentId,
                        "FIRETEAM_EXTINGUISHER_QUIZ"
                )
                        || existsFireExtinguisherQuizCorrectAction(
                        scenarioId,
                        studentId
                );

        boolean fireteamDonutCompleted =
                existsDonutAction(
                        scenarioId,
                        studentId
                )
                        || existsCompletedTeamMissionForStudent(
                        scenarioId,
                        studentId,
                        "FIRETEAM_PUT_OUT_FIRE"
                );

        double roleScore = 0.0;
        if (fireteamExtinguisherAcquired) {
            roleScore += 10.0;
        }
        if (fireteamExtinguisherQuizCompleted) {
            roleScore += 20.0;
        }
        if (fireteamDonutCompleted) {
            roleScore += 10.0;
        }

        double total = quizScore + roleScore + personalScore + safezoneScore;

        return StudentScoreBreakdown.builder()
                .scenarioId(scenarioId)
                .studentId(studentId)

                .quiz(quizScore)
                .role(roleScore)
                .personal(personalScore)
                .safezone(safezoneScore)
                .total(total)

                .correctQuizCount(correctQuizCount)

                .randomQuizCompleted(randomQuizSuccess)
                .reportCallCompleted(reportCallCompleted)
                .extinguisherFound(extinguisherFound)
                .safeZoneCompleted(safeZoneCompleted)

                .fireteamExtinguisherAcquired(fireteamExtinguisherAcquired)
                .fireteamExtinguisherQuizCompleted(fireteamExtinguisherQuizCompleted)
                .fireteamDonutCompleted(fireteamDonutCompleted)

                .build();
    }

    private int countCorrectQuizSubmissions(String scenarioId, String studentId) {
        String sql = """
            select count(*)
            from quiz_submissions qs
            join scenario_assignments sa
              on qs.assignment_id = sa.id
            where qs.scenario_id = :scenarioId
              and qs.student_id = :studentId
              and sa.assignment_type = 'QUIZ'
              and qs.is_correct = 1
        """;

        return toInt(
                entityManager.createNativeQuery(sql)
                        .setParameter("scenarioId", scenarioId)
                        .setParameter("studentId", studentId)
                        .getSingleResult()
        );
    }

    private boolean existsCompletedMissionCode(
            String scenarioId,
            String studentId,
            String missionCode
    ) {
        String sql = """
        select count(*)
        from student_mission_progress smp
        join scenario_assignments sa
          on smp.assignment_id = sa.id
        left join contents c
          on sa.content_id = c.id
        where smp.scenario_id = :scenarioId
          and smp.student_id = :studentId
          and smp.status = 'COMPLETED'
          and (
                (
                    case
                        when json_valid(sa.params_json)
                        then json_unquote(json_extract(sa.params_json, '$.missionCode'))
                        else null
                    end
                ) = :missionCode

                or coalesce(sa.params_json, '') like concat('%', :missionCode, '%')

                or (
                    :missionCode = 'COMMON_REPORT_CALL'
                    and c.title in ('전화 미션', '119 신고 순서 맞추기')
                )
                or (
                    :missionCode = 'COMMON_FIND_EXTINGUISHER'
                    and c.title = '소화기 찾기'
                )
                or (
                    :missionCode = 'COMMON_SAFE_ZONE'
                    and c.title = '제한 시간 내 안전구역 도착'
                )
                or (
                    :missionCode = 'COMMON_RANDOM_QUIZ'
                    and c.title = '랜덤 퀴즈 3개 이상 맞추기'
                )
                or (
                    :missionCode = 'FIRETEAM_GET_EXTINGUISHER'
                    and c.title = '소화팀: 소화기 획득'
                )
                or (
                    :missionCode = 'FIRETEAM_EXTINGUISHER_QUIZ'
                    and c.title = '소화팀: 소화기 사용 퀴즈'
                )
                or (
                    :missionCode = 'FIRETEAM_PUT_OUT_FIRE'
                    and c.title = '소화팀: 도넛 게임으로 불 끄기'
                )
          )
    """;

        return toInt(
                entityManager.createNativeQuery(sql)
                        .setParameter("scenarioId", scenarioId)
                        .setParameter("studentId", studentId)
                        .setParameter("missionCode", missionCode)
                        .getSingleResult()
        ) > 0;
    }

    private boolean existsNotConsumedExtinguisherItem(
            String scenarioId,
            String studentId
    ) {
        String sql = """
            select count(*)
            from student_items si
            join items i
              on si.item_id = i.id
            where si.scenario_id = :scenarioId
              and si.student_id = :studentId
              and si.is_consumed = 0
              and (
                    upper(i.item_code) = 'EXTINGUISHER'
                    or upper(i.item_code) = 'FIRE_EXTINGUISHER'
                    or upper(i.item_code) like '%EXTINGUISH%'
                    or upper(i.item_type) like '%EXTINGUISH%'
              )
        """;

        return toInt(
                entityManager.createNativeQuery(sql)
                        .setParameter("scenarioId", scenarioId)
                        .setParameter("studentId", studentId)
                        .getSingleResult()
        ) > 0;
    }

    private boolean existsPickupFireExtinguisherAction(
            String scenarioId,
            String studentId
    ) {
        String sql = """
            select count(*)
            from scenario_action_events e
            where e.scenario_id = :scenarioId
              and e.student_id = :studentId
              and e.action_type = 'PICKUP_ITEM'
              and (
                    e.value_text = 'FIRE_EXTINGUISHER'
                    or e.element_id = 'FIRE_EXTINGUISHER'
                    or coalesce(e.meta_json, '') like '%FIRETEAM_GET_EXTINGUISHER%'
                    or coalesce(e.meta_json, '') like '%FIRE_EXTINGUISHER%'
              )
        """;

        return toInt(
                entityManager.createNativeQuery(sql)
                        .setParameter("scenarioId", scenarioId)
                        .setParameter("studentId", studentId)
                        .getSingleResult()
        ) > 0;
    }

    private boolean existsFireExtinguisherQuizCorrectAction(
            String scenarioId,
            String studentId
    ) {
        String sql = """
            select count(*)
            from scenario_action_events e
            where e.scenario_id = :scenarioId
              and e.student_id = :studentId
              and e.action_type in ('QUIZ_SUBMIT', 'CARD_QUIZ_SUBMIT', 'MISSION_COMPLETE')
              and (
                    e.value_text = 'FIRETEAM_EXTINGUISHER_QUIZ'
                    or coalesce(e.meta_json, '') like '%FIRETEAM_EXTINGUISHER_QUIZ%'
              )
              and (
                    e.action_type = 'MISSION_COMPLETE'
                    or e.value_text = 'CORRECT'
                    or coalesce(e.meta_json, '') like '%"isCorrect":true%'
                    or coalesce(e.meta_json, '') like '%"is_correct":true%'
              )
        """;

        return toInt(
                entityManager.createNativeQuery(sql)
                        .setParameter("scenarioId", scenarioId)
                        .setParameter("studentId", studentId)
                        .getSingleResult()
        ) > 0;
    }

    private boolean existsDonutAction(
            String scenarioId,
            String studentId
    ) {
        String sql = """
            select count(*)
            from scenario_action_events e
            where e.scenario_id = :scenarioId
              and e.student_id = :studentId
              and e.action_type in ('MISSION_INCREMENT', 'MISSION_COMPLETE')
              and (
                    e.value_text = 'FIRETEAM_PUT_OUT_FIRE'
                    or e.element_id = 'FIRE_DONUT'
                    or coalesce(e.meta_json, '') like '%FIRETEAM_PUT_OUT_FIRE%'
                    or coalesce(e.meta_json, '') like '%FIRE_DONUT%'
              )
        """;

        return toInt(
                entityManager.createNativeQuery(sql)
                        .setParameter("scenarioId", scenarioId)
                        .setParameter("studentId", studentId)
                        .getSingleResult()
        ) > 0;
    }

    private boolean existsCompletedTeamMissionForStudent(
            String scenarioId,
            String studentId,
            String missionCode
    ) {
        String sql = """
            select count(*)
            from team_mission_progress tmp
            join scenario_assignments sa
              on tmp.assignment_id = sa.id
            join scenario_team_members stm
              on stm.scenario_id = tmp.scenario_id
             and stm.team_id = tmp.team_id
            where tmp.scenario_id = :scenarioId
              and stm.student_id = :studentId
              and tmp.status = 'COMPLETED'
              and coalesce(sa.params_json, '') like concat('%', :missionCode, '%')
        """;

        return toInt(
                entityManager.createNativeQuery(sql)
                        .setParameter("scenarioId", scenarioId)
                        .setParameter("studentId", studentId)
                        .setParameter("missionCode", missionCode)
                        .getSingleResult()
        ) > 0;
    }

    private int toInt(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.parseInt(String.valueOf(value));
    }
}