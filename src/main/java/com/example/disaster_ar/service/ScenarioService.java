package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.ScenarioV4;
import com.example.disaster_ar.domain.v4.enums.NpcMode;
import com.example.disaster_ar.domain.v4.enums.ScenarioType;
import com.example.disaster_ar.domain.v4.enums.TeamMode;
import com.example.disaster_ar.domain.v4.enums.TriggerMode;
import com.example.disaster_ar.dto.scenario.ScenarioCreateRequest;
import com.example.disaster_ar.dto.scenario.ScenarioResponse;
import com.example.disaster_ar.dto.scenario.ScenarioUpdateRequest;
import com.example.disaster_ar.repository.ClassroomRepository;
import com.example.disaster_ar.repository.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ClassroomRepository classroomRepository;

    public ScenarioResponse create(ScenarioCreateRequest req) {

        ClassroomV4 classroom = classroomRepository.findById(req.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("교실이 존재하지 않습니다."));

        ScenarioV4 s = new ScenarioV4();
        if (s.getId() == null) s.setId(UUID.randomUUID().toString());

        s.setClassroom(classroom);
        s.setScenarioName(req.getScenarioName());
        s.setScenarioType(parseEnum(ScenarioType.class, req.getScenarioType(), "scenarioType"));

        if (req.getTriggerMode() != null)
            s.setTriggerMode(parseEnum(TriggerMode.class, req.getTriggerMode(), "triggerMode"));
        if (req.getTeamMode() != null)
            s.setTeamMode(parseEnum(TeamMode.class, req.getTeamMode(), "teamMode"));
        if (req.getNpcMode() != null)
            s.setNpcMode(parseEnum(NpcMode.class, req.getNpcMode(), "npcMode"));

        s.setLocation(req.getLocation());
        s.setIntensity(req.getIntensity());
        s.setTrainTime(req.getTrainTime());
        s.setTeamAssignmentJson(req.getTeamAssignment()); // ✅ 수정
        s.setNpcPositionsJson(req.getNpcPositions());     // ✅ 수정
        s.setParticipantCount(req.getParticipantCount());
        s.setCreatedTime(LocalDateTime.now());

        scenarioRepository.save(s);
        return toDto(s);
    }

    public ScenarioResponse update(ScenarioUpdateRequest req) {
        ScenarioV4 s = scenarioRepository.findById(req.getScenarioId())
                .orElseThrow(() -> new IllegalArgumentException("시나리오 없음"));

        if (req.getScenarioName() != null) s.setScenarioName(req.getScenarioName());
        if (req.getScenarioType() != null)
            s.setScenarioType(parseEnum(ScenarioType.class, req.getScenarioType(), "scenarioType"));
        if (req.getTriggerMode() != null)
            s.setTriggerMode(parseEnum(TriggerMode.class, req.getTriggerMode(), "triggerMode"));
        if (req.getTeamMode() != null)
            s.setTeamMode(parseEnum(TeamMode.class, req.getTeamMode(), "teamMode"));
        if (req.getNpcMode() != null)
            s.setNpcMode(parseEnum(NpcMode.class, req.getNpcMode(), "npcMode"));

        if (req.getLocation() != null) s.setLocation(req.getLocation());
        if (req.getIntensity() != null) s.setIntensity(req.getIntensity());
        if (req.getTrainTime() != null) s.setTrainTime(req.getTrainTime());
        if (req.getTeamAssignment() != null) s.setTeamAssignmentJson(req.getTeamAssignment()); // ✅ 수정
        if (req.getNpcPositions() != null) s.setNpcPositionsJson(req.getNpcPositions());       // ✅ 수정
        if (req.getParticipantCount() != null) s.setParticipantCount(req.getParticipantCount());

        scenarioRepository.save(s);
        return toDto(s);
    }

    public List<ScenarioResponse> listByClassroom(String classroomId) {
        return scenarioRepository.findByClassroom_IdOrderByCreatedTimeDesc(classroomId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private ScenarioResponse toDto(ScenarioV4 s) {
        ScenarioResponse r = new ScenarioResponse();
        r.setId(s.getId());
        r.setClassroomId(s.getClassroom().getId());
        r.setScenarioName(s.getScenarioName());

        r.setScenarioType(s.getScenarioType() != null ? s.getScenarioType().name() : null);
        r.setTriggerMode(s.getTriggerMode() != null ? s.getTriggerMode().name() : null);
        r.setTeamMode(s.getTeamMode() != null ? s.getTeamMode().name() : null);
        r.setNpcMode(s.getNpcMode() != null ? s.getNpcMode().name() : null);

        r.setLocation(s.getLocation());
        r.setIntensity(s.getIntensity());
        r.setTrainTime(s.getTrainTime());
        r.setTeamAssignmentJson(s.getTeamAssignmentJson()); // ✅ 수정
        r.setNpcPositionsJson(s.getNpcPositionsJson());     // ✅ 수정
        r.setParticipantCount(s.getParticipantCount());
        r.setCreatedTime(s.getCreatedTime());
        return r;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, String fieldName) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " 값 오류: " + raw);
        }
    }
}