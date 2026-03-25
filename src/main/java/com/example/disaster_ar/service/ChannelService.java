package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.ChannelMapV4;
import com.example.disaster_ar.domain.v4.ClassroomV4;
import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.domain.v4.StudentV4;
import com.example.disaster_ar.domain.v4.enums.StudentStatus;
import com.example.disaster_ar.dto.channel.JoinClassroomRequest;
import com.example.disaster_ar.dto.channel.JoinClassroomResponse;
import com.example.disaster_ar.dto.channel.JoinSchoolResponse;
import com.example.disaster_ar.repository.ChannelMapRepositoryV4;
import com.example.disaster_ar.repository.ClassroomRepository;
import com.example.disaster_ar.repository.SchoolRepository;
import com.example.disaster_ar.repository.StudentRepositoryV4;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelService {

    private final SchoolRepository schoolRepository;
    private final ClassroomRepository classroomRepository;
    private final StudentRepositoryV4 studentRepository;
    private final ChannelMapRepositoryV4 channelMapRepositoryV4;

    public SchoolV4 createChannel(String schoolName,
                                  List<MultipartFile> mapImages,
                                  String uploadDir) {

        SchoolV4 school = schoolRepository.findBySchoolName(schoolName)
                .orElseGet(() -> {
                    SchoolV4 s = new SchoolV4();
                    if (s.getId() == null) s.setId(UUID.randomUUID().toString());
                    s.setSchoolName(schoolName);
                    s.setAccessCode(generateAccessCode());
                    return s;
                });

        if (!schoolRepository.existsById(school.getId())) {
            school = schoolRepository.save(school);
        }

        if (mapImages != null && !mapImages.isEmpty()) {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                throw new RuntimeException("지도 업로드 폴더 생성 실패", e);
            }

            int floorIndex = 0;
            List<String> storedPaths = new ArrayList<>();

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

                ChannelMapV4 cm = new ChannelMapV4();
                if (cm.getId() == null) cm.setId(UUID.randomUUID().toString());

                cm.setSchool(school);
                cm.setFloorIndex(floorIndex);
                cm.setFloorLabel(null);
                cm.setUploadedImage(webPath);
                cm.setOutlineJson(null);
                cm.setScaleMPerPx(null);
                cm.setOriginX(null);
                cm.setOriginY(null);
                cm.setElementsJson("[]");
                cm.setUpdatedAt(LocalDateTime.now());

                channelMapRepositoryV4.save(cm);
                floorIndex++;
            }

            if (!storedPaths.isEmpty()) {
                school.setThumbnailImage(storedPaths.get(0));
                schoolRepository.save(school);
            }
        }

        return school;
    }

    public String getRoomCode(String schoolId) {
        SchoolV4 school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));
        return school.getAccessCode();
    }

    public String regenerateRoomCode(String schoolId) {
        SchoolV4 school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("학교가 존재하지 않습니다."));
        school.setAccessCode(generateAccessCode());
        schoolRepository.save(school);
        return school.getAccessCode();
    }

    public JoinSchoolResponse joinByAccessCode(String accessCode) {
        SchoolV4 school = schoolRepository.findByAccessCode(accessCode)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 채널 코드입니다."));

        List<String> mapFiles = channelMapRepositoryV4
                .findBySchool_IdOrderByFloorIndexAsc(school.getId())
                .stream()
                .map(ChannelMapV4::getUploadedImage)
                .filter(p -> p != null && !p.isBlank())
                .toList();

        JoinSchoolResponse res = new JoinSchoolResponse();
        res.setSchoolId(school.getId());
        res.setSchoolName(school.getSchoolName());
        res.setChannelCode(school.getAccessCode());
        res.setMapFiles(mapFiles);
        return res;
    }

    @Transactional
    public JoinClassroomResponse joinRoom(JoinClassroomRequest req) {
        ClassroomV4 classroom = classroomRepository.findByJoinCode(req.getJoinCode())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 방 입장 코드입니다."));

        StudentV4 student = StudentV4.builder()
                .id(UUID.randomUUID().toString())
                .classroom(classroom)
                .studentName(req.getStudentName())
                .joinedAt(LocalDateTime.now())
                .status(StudentStatus.UNKNOWN)
                .isKicked(false)
                .build();

        studentRepository.save(student);

        return JoinClassroomResponse.builder()
                .studentId(student.getId())
                .classroomId(classroom.getId())
                .className(classroom.getClassName())
                .trainingState(
                        classroom.getTrainingState() != null
                                ? classroom.getTrainingState().name()
                                : null
                )
                .joinedAt(student.getJoinedAt())
                .build();
    }

    private String generateAccessCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}