package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.Classroom;
import com.example.disaster_ar.domain.Scenario;
import com.example.disaster_ar.domain.enums.NpcMode;
import com.example.disaster_ar.domain.enums.ScenarioType;
import com.example.disaster_ar.domain.enums.TeamMode;
import com.example.disaster_ar.domain.enums.TriggerMode;
import com.example.disaster_ar.dto.scenario.ScenarioCreateRequest;
import com.example.disaster_ar.dto.scenario.ScenarioResponse;
import com.example.disaster_ar.dto.scenario.ScenarioUpdateRequest;
import com.example.disaster_ar.repository.ClassroomRepository;
import com.example.disaster_ar.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ClassroomRepository classroomRepository;

    /**
     * 시나리오 생성
     */
    public ScenarioResponse create(ScenarioCreateRequest req) {

        Classroom classroom = classroomRepository.findById(req.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        Scenario s = new Scenario();
        s.setClassroom(classroom);
        s.setScenarioName(req.getScenarioName());

        // 필수 enum (scenarioType)
        s.setScenarioType(parseEnum(ScenarioType.class, req.getScenarioType(), "scenarioType"));

        // 나머지 enum 은 null 허용 → @PrePersist 에서 DEFAULT 설정
        if (req.getTriggerMode() != null) {
            s.setTriggerMode(parseEnum(TriggerMode.class, req.getTriggerMode(), "triggerMode"));
        }
        if (req.getTeamMode() != null) {
            s.setTeamMode(parseEnum(TeamMode.class, req.getTeamMode(), "teamMode"));
        }
        if (req.getNpcMode() != null) {
            s.setNpcMode(parseEnum(NpcMode.class, req.getNpcMode(), "npcMode"));
        }

        s.setLocation(req.getLocation());
        s.setIntensity(req.getIntensity());
        s.setTrainTime(req.getTrainTime());
        s.setTeamAssignment(req.getTeamAssignment());
        s.setNpcPositions(req.getNpcPositions());
        s.setParticipantCount(req.getParticipantCount());

        scenarioRepository.save(s);
        return toDto(s);
    }

    /**
     * 시나리오 수정 (부분 업데이트)
     */
    public ScenarioResponse update(ScenarioUpdateRequest req) {
        Scenario s = scenarioRepository.findById(req.getScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        if (req.getScenarioName() != null) {
            s.setScenarioName(req.getScenarioName());
        }
        if (req.getScenarioType() != null) {
            s.setScenarioType(parseEnum(ScenarioType.class, req.getScenarioType(), "scenarioType"));
        }
        if (req.getTriggerMode() != null) {
            s.setTriggerMode(parseEnum(TriggerMode.class, req.getTriggerMode(), "triggerMode"));
        }
        if (req.getTeamMode() != null) {
            s.setTeamMode(parseEnum(TeamMode.class, req.getTeamMode(), "teamMode"));
        }
        if (req.getNpcMode() != null) {
            s.setNpcMode(parseEnum(NpcMode.class, req.getNpcMode(), "npcMode"));
        }
        if (req.getLocation() != null) {
            s.setLocation(req.getLocation());
        }
        if (req.getIntensity() != null) {
            s.setIntensity(req.getIntensity());
        }
        if (req.getTrainTime() != null) {
            s.setTrainTime(req.getTrainTime());
        }
        if (req.getTeamAssignment() != null) {
            s.setTeamAssignment(req.getTeamAssignment());
        }
        if (req.getNpcPositions() != null) {
            s.setNpcPositions(req.getNpcPositions());
        }
        if (req.getParticipantCount() != null) {
            s.setParticipantCount(req.getParticipantCount());
        }

        scenarioRepository.save(s);
        return toDto(s);
    }

    /**
     * 방(교실)별 시나리오 목록 조회
     */
    public List<ScenarioResponse> listByClassroom(String classroomId) {
        // ⚠️ ScenarioRepository에 아래 메서드가 있어야 함:
        // List<Scenario> findByClassroom_IdOrderByCreatedTimeDesc(String classroomId);
        return scenarioRepository.findByClassroom_IdOrderByCreatedTimeDesc(classroomId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ScenarioResponse toDto(Scenario s) {
        ScenarioResponse r = new ScenarioResponse();
        r.setId(s.getId());
        r.setClassroomId(s.getClassroom().getId());
        r.setScenarioName(s.getScenarioName());
        r.setScenarioType(s.getScenarioType().name());
        r.setTriggerMode(s.getTriggerMode().name());
        r.setTeamMode(s.getTeamMode().name());
        r.setNpcMode(s.getNpcMode().name());
        r.setLocation(s.getLocation());
        r.setIntensity(s.getIntensity());
        r.setTrainTime(s.getTrainTime());
        r.setTeamAssignmentJson(s.getTeamAssignment());
        r.setNpcPositionsJson(s.getNpcPositions());
        r.setParticipantCount(s.getParticipantCount());
        r.setCreatedTime(s.getCreatedTime());
        return r;
    }

    /**
     * 문자열을 ENUM으로 변환 (대소문자 무시)
     */
    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String fieldName) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 값 오류: " + raw);
        }
    }
}
