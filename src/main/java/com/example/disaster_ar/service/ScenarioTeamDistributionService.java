package com.example.disaster_ar.service;

import com.example.disaster_ar.config.TeamRatioConfig;
import com.example.disaster_ar.domain.v4.ScenarioTeamV4;
import com.example.disaster_ar.domain.v4.ScenarioV4;
import com.example.disaster_ar.dto.scenario.TeamDistributionRequest;
import com.example.disaster_ar.dto.scenario.TeamDistributionResponse;
import com.example.disaster_ar.repository.ScenarioRepository;
import com.example.disaster_ar.repository.ScenarioTeamRepositoryV4;
import com.example.disaster_ar.repository.StudentRepositoryV4;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ScenarioTeamDistributionService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioTeamRepositoryV4 scenarioTeamRepositoryV4;
    private final StudentRepositoryV4 studentRepositoryV4;

    @Transactional
    public TeamDistributionResponse distributeTeams(String scenarioId, TeamDistributionRequest req) {
        ScenarioV4 scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다."));

        if (scenario.getClassroom() == null) {
            throw new IllegalArgumentException("시나리오에 연결된 교실이 없습니다.");
        }

        String classroomId = scenario.getClassroom().getId();

        List<?> activeStudents = studentRepositoryV4
                .findByClassroom_IdAndIsKickedFalseOrderByJoinedAtAsc(classroomId);

        int totalStudents = activeStudents.size();

        if (totalStudents <= 0) {
            throw new IllegalArgumentException("배정할 학생이 없습니다.");
        }

        String mode = req != null && req.getDistributionMode() != null
                ? req.getDistributionMode()
                : "AUTO";

        List<TeamDistributionResponse.TeamResult> results;

        if ("MANUAL".equalsIgnoreCase(mode)) {
            results = distributeManual(scenario, totalStudents, req);
        } else {
            results = distributeAuto(scenario, totalStudents);
        }

        return TeamDistributionResponse.builder()
                .scenarioId(scenario.getId())
                .classroomId(classroomId)
                .scenarioType(scenario.getScenarioType().name())
                .totalStudents(totalStudents)
                .teams(results)
                .build();
    }

    private List<TeamDistributionResponse.TeamResult> distributeAuto(
            ScenarioV4 scenario,
            int totalStudents
    ) {
        List<TeamRatioConfig.TeamRatio> ratios =
                TeamRatioConfig.TEAM_RATIO_BY_SCENARIO.get(scenario.getScenarioType());

        if (ratios == null || ratios.isEmpty()) {
            throw new IllegalArgumentException("해당 시나리오 타입의 팀 비율 설정이 없습니다.");
        }

        int totalRatio = ratios.stream()
                .mapToInt(TeamRatioConfig.TeamRatio::ratio)
                .sum();

        List<TempTeamCount> temp = new ArrayList<>();
        int assigned = 0;

        for (TeamRatioConfig.TeamRatio ratio : ratios) {
            double exact = (double) totalStudents * ratio.ratio() / totalRatio;
            int floor = (int) Math.floor(exact);
            double remainder = exact - floor;

            temp.add(new TempTeamCount(
                    ratio.teamCode(),
                    ratio.teamName(),
                    floor,
                    remainder
            ));

            assigned += floor;
        }

        int remaining = totalStudents - assigned;

        temp.sort(Comparator.comparingDouble(TempTeamCount::remainder).reversed());

        for (int i = 0; i < remaining; i++) {
            TempTeamCount t = temp.get(i);
            t.maxMembers(t.maxMembers() + 1);
        }

        temp.sort(Comparator.comparing(TempTeamCount::teamCode));

        List<TeamDistributionResponse.TeamResult> results = new ArrayList<>();

        for (TempTeamCount t : temp) {
            ScenarioTeamV4 team = scenarioTeamRepositoryV4
                    .findByScenario_IdAndTeamCode(scenario.getId(), t.teamCode())
                    .orElseGet(() -> ScenarioTeamV4.builder()
                            .id(UUID.randomUUID().toString())
                            .scenario(scenario)
                            .teamCode(t.teamCode())
                            .teamName(t.teamName())
                            .build()
                    );

            team.setTeamName(t.teamName());
            team.setMinMembers(0);
            team.setMaxMembers(t.maxMembers());

            ScenarioTeamV4 saved = scenarioTeamRepositoryV4.save(team);
            results.add(toTeamResult(saved));
        }

        return results;
    }

    private List<TeamDistributionResponse.TeamResult> distributeManual(
            ScenarioV4 scenario,
            int totalStudents,
            TeamDistributionRequest req
    ) {
        if (req == null || req.getManualTeamCounts() == null || req.getManualTeamCounts().isEmpty()) {
            throw new IllegalArgumentException("MANUAL 모드에서는 manualTeamCounts가 필요합니다.");
        }

        Set<String> seen = new HashSet<>();
        int sum = 0;

        for (TeamDistributionRequest.ManualTeamCount c : req.getManualTeamCounts()) {
            if (c.getTeamCode() == null || c.getTeamCode().isBlank()) {
                throw new IllegalArgumentException("teamCode가 비어 있습니다.");
            }
            if (!seen.add(c.getTeamCode())) {
                throw new IllegalArgumentException("중복된 teamCode가 있습니다: " + c.getTeamCode());
            }
            if (c.getMaxMembers() == null || c.getMaxMembers() <= 0) {
                throw new IllegalArgumentException("팀 인원수는 1 이상이어야 합니다: " + c.getTeamCode());
            }
            sum += c.getMaxMembers();
        }

        if (sum != totalStudents) {
            throw new IllegalArgumentException("수동 팀 인원수 합계가 전체 학생 수와 일치해야 합니다.");
        }

        List<TeamDistributionResponse.TeamResult> results = new ArrayList<>();

        for (TeamDistributionRequest.ManualTeamCount c : req.getManualTeamCounts()) {
            ScenarioTeamV4 team = scenarioTeamRepositoryV4
                    .findByScenario_IdAndTeamCode(scenario.getId(), c.getTeamCode())
                    .orElseGet(() -> ScenarioTeamV4.builder()
                            .id(UUID.randomUUID().toString())
                            .scenario(scenario)
                            .teamCode(c.getTeamCode())
                            .build()
                    );

            team.setTeamName(
                    c.getTeamName() != null && !c.getTeamName().isBlank()
                            ? c.getTeamName()
                            : c.getTeamCode()
            );
            team.setMinMembers(0);
            team.setMaxMembers(c.getMaxMembers());

            ScenarioTeamV4 saved = scenarioTeamRepositoryV4.save(team);
            results.add(toTeamResult(saved));
        }

        return results;
    }

    private TeamDistributionResponse.TeamResult toTeamResult(ScenarioTeamV4 team) {
        return TeamDistributionResponse.TeamResult.builder()
                .teamId(team.getId())
                .teamCode(team.getTeamCode())
                .teamName(team.getTeamName())
                .minMembers(team.getMinMembers())
                .maxMembers(team.getMaxMembers())
                .build();
    }

    private static class TempTeamCount {
        private final String teamCode;
        private final String teamName;
        private int maxMembers;
        private final double remainder;

        private TempTeamCount(String teamCode, String teamName, int maxMembers, double remainder) {
            this.teamCode = teamCode;
            this.teamName = teamName;
            this.maxMembers = maxMembers;
            this.remainder = remainder;
        }

        public String teamCode() {
            return teamCode;
        }

        public String teamName() {
            return teamName;
        }

        public int maxMembers() {
            return maxMembers;
        }

        public void maxMembers(int maxMembers) {
            this.maxMembers = maxMembers;
        }

        public double remainder() {
            return remainder;
        }
    }
}