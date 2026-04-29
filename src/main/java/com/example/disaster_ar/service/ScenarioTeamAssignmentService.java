package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.ScenarioTeamMemberV4;
import com.example.disaster_ar.domain.v4.ScenarioTeamV4;
import com.example.disaster_ar.domain.v4.ScenarioV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.domain.v4.enums.ActorType;
import com.example.disaster_ar.dto.scenario.TeamAssignmentResponse;
import com.example.disaster_ar.repository.ScenarioRepository;
import com.example.disaster_ar.repository.ScenarioTeamMemberRepositoryV4;
import com.example.disaster_ar.repository.ScenarioTeamRepositoryV4;
import com.example.disaster_ar.repository.StudentRepositoryV4;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScenarioTeamAssignmentService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioTeamRepositoryV4 scenarioTeamRepositoryV4;
    private final ScenarioTeamMemberRepositoryV4 scenarioTeamMemberRepositoryV4;
    private final StudentRepositoryV4 studentRepositoryV4;

    @Transactional
    public TeamAssignmentResponse assignStudents(String scenarioId) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (scenario.getClassroom() == null) {
            throw new IllegalArgumentException("시나리오에 연결된 교실이 없습니다.");
        }

        String classroomId = scenario.getClassroom().getId();

        List<StudentV4> students = new ArrayList<>(
                studentRepositoryV4.findByClassroom_IdAndIsKickedFalseOrderByJoinedAtAsc(classroomId)
        );

        if (students.isEmpty()) {
            throw new IllegalArgumentException("배정할 학생이 없습니다.");
        }

        List<ScenarioTeamV4> teams =
                scenarioTeamRepositoryV4.findByScenario_IdOrderByTeamCodeAsc(scenarioId);

        if (teams.isEmpty()) {
            throw new IllegalArgumentException("팀 정원이 먼저 계산되어야 합니다.");
        }

        int totalMax = teams.stream()
                .map(ScenarioTeamV4::getMaxMembers)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        if (totalMax != students.size()) {
            throw new IllegalStateException(
                    "팀 정원 합계와 학생 수가 일치하지 않습니다. 먼저 팀 정원을 다시 계산하세요."
            );
        }

        scenarioTeamMemberRepositoryV4.deleteByScenario_Id(scenarioId);

        Collections.shuffle(students);

        int cursor = 0;
        List<TeamAssignmentResponse.TeamAssignmentResult> teamResults = new ArrayList<>();

        for (ScenarioTeamV4 team : teams) {
            int size = team.getMaxMembers() != null ? team.getMaxMembers() : 0;

            List<TeamAssignmentResponse.StudentResult> assignedStudentResponses = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                StudentV4 student = students.get(cursor++);

                ScenarioTeamMemberV4 member = ScenarioTeamMemberV4.builder()
                        .id(UUID.randomUUID().toString())
                        .scenario(scenario)
                        .team(team)
                        .student(student)
                        .assignedAt(LocalDateTime.now())
                        .assignedByType(ActorType.SYSTEM)
                        .build();

                scenarioTeamMemberRepositoryV4.save(member);

                assignedStudentResponses.add(
                        TeamAssignmentResponse.StudentResult.builder()
                                .studentId(student.getId())
                                .studentName(student.getStudentName())
                                .build()
                );
            }

            teamResults.add(
                    TeamAssignmentResponse.TeamAssignmentResult.builder()
                            .teamId(team.getId())
                            .teamCode(team.getTeamCode())
                            .teamName(team.getTeamName())
                            .maxMembers(team.getMaxMembers())
                            .assignedCount(assignedStudentResponses.size())
                            .students(assignedStudentResponses)
                            .build()
            );
        }

        return TeamAssignmentResponse.builder()
                .scenarioId(scenario.getId())
                .classroomId(classroomId)
                .totalStudents(students.size())
                .teams(teamResults)
                .build();
    }
}