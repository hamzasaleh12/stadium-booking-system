package com.hamza.stadiumbooking.stadium;

import com.hamza.stadiumbooking.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Builder
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class Stadium {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private Double pricePerHour;

    @Column(nullable = false)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private Integer ballRentalFee = 0;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    @Column(name = "last_lock_at")
    private LocalDateTime lastLockAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "stadium_features", joinColumns = @JoinColumn(name = "stadium_id"))
    @Column(name = "feature")
    @Builder.Default
    private Set<String> features = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(insertable = false)
    private LocalDateTime updatedAt;

    public boolean isOpenAt(LocalTime start, LocalTime end) {
        if (closeTime.isAfter(openTime)) {
            return !start.isBefore(openTime) && !end.isAfter(closeTime);
        }
        else {
            if (start.isAfter(end)) return !start.isBefore(openTime) && !end.isAfter(closeTime);
            else return !start.isBefore(openTime) || !end.isAfter(closeTime);
        }
    }
}