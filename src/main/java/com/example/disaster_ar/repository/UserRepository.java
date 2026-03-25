package com.example.disaster_ar.repository;

import com.example.disaster_ar.domain.v4.UserV4;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserV4, String> {
    Optional<UserV4> findByEmail(String email);
}