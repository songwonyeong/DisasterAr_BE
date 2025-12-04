package com.example.disaster_ar.service;

import com.example.disaster_ar.domain.School;
import com.example.disaster_ar.domain.User;
import com.example.disaster_ar.domain.enums.UserRole;
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

        // 2) 학교 조회/생성 (동시성 방어 포함)
        School school = schoolRepository.findBySchoolName(req.getSchoolName())
                .orElseGet(() -> {
                    School s = new School();                         // ✅ id 수동 세팅 금지
                    s.setSchoolName(req.getSchoolName());
                    s.setAccessCode(generateAccessCode());
                    try {
                        return schoolRepository.saveAndFlush(s);     // 즉시 INSERT 시도
                    } catch (DataIntegrityViolationException e) {
                        // UNIQUE(school_name) 충돌 시 다른 트랜잭션이 선점했을 수 있으므로 재조회
                        return schoolRepository.findBySchoolName(req.getSchoolName())
                                .orElseThrow(() -> e);
                    }
                });

        // 3) 사용자 생성 (id 수동 세팅 금지 → @GeneratedValue 전략으로 INSERT)
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setRole(UserRole.TEACHER);
        user.setSchool(school);

        userRepository.save(user); // INSERT

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
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String schoolId = Optional.ofNullable(user.getSchool()).map(School::getId).orElse(null);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                schoolId
        );
    }

    private String generateAccessCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
