package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.v4.SchoolV4;
import com.example.disaster_ar.domain.v4.UserV4;
import com.example.disaster_ar.domain.v4.enums.UserRole;
import com.example.disaster_ar.dto.auth.AuthResponse;
import com.example.disaster_ar.dto.auth.LoginRequest;
import com.example.disaster_ar.dto.auth.SignupRequest;
import com.example.disaster_ar.repository.SchoolRepository;
import com.example.disaster_ar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse signup(SignupRequest req) {
        // 1) 이메일 중복 체크
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
        }

        // 2) 학교 조회/생성 (동시성 방어)
        SchoolV4 school = schoolRepository.findBySchoolName(req.getSchoolName())
                .orElseGet(() -> {
                    SchoolV4 s = new SchoolV4();
                    // id 생성 전략이 @GeneratedValue(UUID)가 아니면 필수
                    if (s.getId() == null) s.setId(UUID.randomUUID().toString());

                    s.setSchoolName(req.getSchoolName());
                    s.setAccessCode(generateAccessCode());

                    try {
                        return schoolRepository.saveAndFlush(s);
                    } catch (DataIntegrityViolationException e) {
                        // UNIQUE(school_name) 충돌 시 재조회
                        return schoolRepository.findBySchoolName(req.getSchoolName())
                                .orElseThrow(() -> e);
                    }
                });

        // 3) 사용자 생성
        UserV4 user = new UserV4();
        if (user.getId() == null) user.setId(UUID.randomUUID().toString());

        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setRole(UserRole.TEACHER);

        userRepository.save(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                school.getId()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        UserV4 user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // V4 User에 school FK가 없다면(네 V4 DDL users에는 school_id 없음) → null 반환이 정상
        String schoolId = null;

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                schoolId
        );
    }

    private String generateAccessCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (schoolRepository.findByAccessCode(code).isPresent());
        return code;
    }
}