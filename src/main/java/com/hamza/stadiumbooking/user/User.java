package com.hamza.stadiumbooking.user;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

@Entity
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_user_phone", columnNames = {"phoneNumber"})
        }
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private  String email;

    @Column(nullable = false)
    private  String phoneNumber;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDate dob;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_PLAYER;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    public Integer getAge(){
        return Period.between(dob,LocalDate.now()).getYears();
    }
}