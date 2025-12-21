package com.hamza.stadiumbooking.stadium;

import com.hamza.stadiumbooking.booking.Booking;
import com.hamza.stadiumbooking.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Builder
@Entity
@Table(name = "stadiums")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Stadium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @ManyToOne
    @JoinColumn(name = "owner_id",nullable = false)
    private User owner;

    @OneToMany(mappedBy = "stadium", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    @Column(nullable = false)
    @Builder.Default
    private boolean isDeleted = false;
}