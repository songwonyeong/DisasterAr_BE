package com.example.disaster_ar.domain.v4;

import com.example.disaster_ar.domain.v4.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_users_email", columnNames = "email")
        }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class UserV4 extends BaseTimeEntity {

    @Id
    @Column(length = 36)
    private String id; // UUID 문자열(서비스에서 생성해서 넣는 방식 추천)

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password; // V4 DDL: NULL 허용

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;
}