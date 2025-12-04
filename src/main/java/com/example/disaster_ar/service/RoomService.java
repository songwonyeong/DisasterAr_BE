package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.Classroom;
import com.example.disaster_ar.domain.School;
import com.example.disaster_ar.domain.User;
import com.example.disaster_ar.dto.room.RoomCreateRequest;
import com.example.disaster_ar.dto.room.RoomResponse;
import com.example.disaster_ar.repository.ClassroomRepository;
import com.example.disaster_ar.repository.SchoolRepository;
import com.example.disaster_ar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.disaster_ar.dto.room.RoomUpdateRequest;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final ClassroomRepository classroomRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    /**
     * 방 생성 후 joinCode 반환
     */
    public RoomResponse createRoom(RoomCreateRequest req) {

        School school = schoolRepository.findById(req.getSchoolId())
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        User owner = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));

        Classroom c = new Classroom();

        c.setSchool(school);
        c.setOwner(owner);
        c.setClassName(req.getClassName());
        c.setStudentCount(0);
        c.setJoinCode(generateJoinCode());

        classroomRepository.save(c);
        return toDto(c);
    }

    /**
     * 학교(채널) 기준 방 목록 조회
     */
    public List<RoomResponse> listBySchool(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));

        return classroomRepository.findBySchool(school).stream()
                .map(this::toDto)
                .toList();
    }

    private RoomResponse toDto(Classroom c) {
        RoomResponse r = new RoomResponse();
        r.setClassroomId(c.getId());
        r.setSchoolId(c.getSchool().getId());
        r.setClassName(c.getClassName());
        r.setStudentCount(c.getStudentCount());
        r.setJoinCode(c.getJoinCode());
        return r;
    }

    private String generateJoinCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    /**
     * 방 정보 수정 (방 만든 사람만 가능)
     */
    public RoomResponse updateRoom(RoomUpdateRequest req) {
        Classroom c = classroomRepository.findById(req.getClassroomId())
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        // 권한 체크: 방 만든 사람만 수정 가능
        if (!c.getOwner().getId().equals(req.getUserId())) {
            throw new IllegalArgumentException("이 방을 수정할 권한이 없습니다.");
        }

        if (req.getClassName() != null) {
            c.setClassName(req.getClassName());
        }
        if (req.getStudentCount() != null) {
            c.setStudentCount(req.getStudentCount());
        }

        Classroom saved = classroomRepository.save(c);
        return toDto(saved);
    }

    /**
     * 방 삭제 (방 만든 사람만 가능)
     */
    public void deleteRoom(String classroomId, String userId) {
        Classroom c = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (!c.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("이 방을 삭제할 권한이 없습니다.");
        }

        classroomRepository.delete(c);
    }

    /**
     * 방 입장 코드 재발급 (방 만든 사람만 가능)
     */
    public RoomResponse regenerateJoinCode(String classroomId, String userId) {
        Classroom c = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다."));

        if (!c.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("이 방의 코드를 재발급할 권한이 없습니다.");
        }

        c.setJoinCode(generateJoinCode());
        Classroom saved = classroomRepository.save(c);
        return toDto(saved);
    }
}
