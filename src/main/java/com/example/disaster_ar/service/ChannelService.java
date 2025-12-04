package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.Classroom;
import com.example.disaster_ar.domain.School;
import com.example.disaster_ar.domain.SchoolMap;
import com.example.disaster_ar.dto.channel.JoinClassroomResponse;
import com.example.disaster_ar.dto.channel.JoinSchoolResponse;
import com.example.disaster_ar.repository.ClassroomRepository;
import com.example.disaster_ar.repository.SchoolMapRepository;
import com.example.disaster_ar.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final SchoolRepository schoolRepository;
    private final ClassroomRepository classroomRepository;
    private final SchoolMapRepository schoolMapRepository;

    /**
     * 채널(학교) 생성 + 지도 이미지 업로드
     *
     * - schoolName : 채널(학교) 이름
     * - mapImages  : 업로드할 건물 구조도 이미지들
     * - uploadDir  : 실제 서버 로컬 경로 (예: "uploads/maps")
     */
    public School createChannel(String schoolName,
                                List<MultipartFile> mapImages,
                                String uploadDir) {

        // 같은 이름의 학교가 있으면 재사용, 없으면 새로 생성
        School school = schoolRepository.findBySchoolName(schoolName)
                .orElseGet(() -> {
                    School s = new School();
                    s.setId(UUID.randomUUID().toString());
                    s.setSchoolName(schoolName);
                    s.setAccessCode(generateAccessCode()); // 채널 코드
                    return s;
                });

        // 아직 DB에 없는 경우 먼저 저장
        if (!schoolRepository.existsById(school.getId())) {
            school = schoolRepository.save(school);
        }

        List<String> storedPaths = new ArrayList<>();

        // 지도 이미지 처리
        if (mapImages != null && !mapImages.isEmpty()) {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException("지도 업로드 폴더 생성 실패", e);
            }

            for (MultipartFile mapImage : mapImages) {
                if (mapImage == null || mapImage.isEmpty()) continue;

                String filename = school.getId() + "_" + UUID.randomUUID()
                        + "_" + mapImage.getOriginalFilename();
                Path target = dir.resolve(filename);

                try {
                    mapImage.transferTo(target.toFile());
                } catch (IOException e) {
                    throw new RuntimeException("지도 파일 저장 실패: " + filename, e);
                }

                String webPath = "/uploads/maps/" + filename;
                storedPaths.add(webPath);

                // school_maps 테이블에 한 장씩 저장
                SchoolMap schoolMap = new SchoolMap();
                schoolMap.setSchool(school);
                schoolMap.setMapFile(webPath);
                // 층 정보(floor)는 지금은 null, 나중에 프론트에서 받으면 세팅
                schoolMapRepository.save(schoolMap);
            }

            // 대표 이미지 1개를 School.mapFile 에 저장(첫 번째 것)
            if (!storedPaths.isEmpty()) {
                school.setMapFile(storedPaths.get(0));
            }
        }

        return schoolRepository.save(school);
    }

    /**
     * 채널 코드 조회 (schoolId -> accessCode)
     */
    public String getRoomCode(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));
        return school.getAccessCode();
    }

    /**
     * 채널 코드 재발급
     */
    public String regenerateRoomCode(String schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));
        school.setAccessCode(generateAccessCode());
        schoolRepository.save(school);
        return school.getAccessCode();
    }

    /**
     * 채널 코드(= accessCode)로 채널 입장
     * DTO에서는 channelCode 라고 부르지만, 값 자체는 accessCode와 동일
     */
    public JoinSchoolResponse joinByAccessCode(String accessCode) {
        School school = schoolRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 채널 코드입니다."));

        // 해당 학교의 지도 리스트 조회
        List<SchoolMap> maps = schoolMapRepository.findBySchool(school);
        List<String> mapFiles = maps.stream()
                .map(SchoolMap::getMapFile)
                .toList();

        JoinSchoolResponse res = new JoinSchoolResponse();
        res.setSchoolId(school.getId());
        res.setSchoolName(school.getSchoolName());
        res.setChannelCode(school.getAccessCode());
        res.setMapFiles(mapFiles);
        return res;
    }

    /**
     * 방 입장 코드(joinCode)로 방(Room, Classroom) 입장
     */
    public JoinClassroomResponse joinByJoinCode(String joinCode) {
        Classroom classroom = classroomRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 방 입장 코드입니다."));

        JoinClassroomResponse res = new JoinClassroomResponse();
        res.setClassroomId(classroom.getId());
        res.setClassName(classroom.getClassName());
        res.setStudentCount(classroom.getStudentCount());
        res.setJoinCode(classroom.getJoinCode());

        if (classroom.getSchool() != null) {
            res.setSchoolId(classroom.getSchool().getId());
        }

        return res;
    }

    private String generateAccessCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
