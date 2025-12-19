package com.hamza.stadiumbooking.stadium;


import com.hamza.stadiumbooking.user.Role;
import com.hamza.stadiumbooking.user.User;
import com.hamza.stadiumbooking.user.UserRepository;
import org.hibernate.mapping.Array;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class StadiumRepositoryTest {

    @Autowired
    private StadiumRepository stadiumRepository;
    @Autowired
    private UserRepository userRepository;

    private User savedUser;
    private Stadium savedStadium;

    @BeforeEach
    void setUp(){
        User user = new User(
                null,0L, "hamza", "manger@gmail.com", "01000000000",
                "12345", LocalDate.now(), Role.ROLE_MANAGER, null, false
        );
        savedUser = userRepository.save(user);
        Stadium stadium = new Stadium(
                null,1L, "AL-AHLY", "Nasr-City", 500.00,
                "image.com", Type.ELEVEN_A_SIDE, 50, savedUser, null,false
        );
        savedStadium = stadiumRepository.save(stadium);
    }

    @Test
    void existsByIdAndOwnerId() {
        boolean test = stadiumRepository.existsByIdAndOwner_Id(savedStadium.getId(),savedUser.getId());

        assertThat(test).isTrue();
    }
    @Test
    void notExistsByIdAndOwnerId() {
        boolean test = stadiumRepository.existsByIdAndOwner_Id(savedStadium.getId(), 9999L);

        assertThat(test).isFalse();
    }
    @Test
    void findAllByIsDeletedFalse() {

        Page<Stadium> test = stadiumRepository.findAllByIsDeletedFalse(Pageable.unpaged());

        assertThat(test).hasSize(1);
    }
    @Test
    void findAllByIsDeletedFalse_ShouldReturnEmpty() {
        savedStadium.setDeleted(true);
        stadiumRepository.save(savedStadium);

        Page<Stadium> test = stadiumRepository.findAllByIsDeletedFalse(Pageable.unpaged());

        assertThat(test).isEmpty();
    }
    @Test
    void findByIdAndIsDeletedFalse() {

        Optional<Stadium> test = stadiumRepository.findByIdAndIsDeletedFalse(savedStadium.getId());

        assertThat(test.get().getId()).isEqualTo(savedStadium.getId());
    }
    @Test
    void findByIdAndIsDeletedFalse_ShouldReturnEmpty() {
        savedStadium.setDeleted(true);
        stadiumRepository.save(savedStadium);

        Optional<Stadium> test = stadiumRepository.findByIdAndIsDeletedFalse(savedStadium.getId());

        assertThat(test).isEmpty();
    }
    @Test
    void findAllDistinctLocations(){
        Stadium duplicateLocationStadium = new Stadium(
                null, 2L, "Zamalek-Club", "Nasr-City", 600.00,
                "img2.com", Type.FIVE_A_SIDE, 30, savedUser, null, false
        );
        stadiumRepository.save(duplicateLocationStadium);

        Stadium differentLocationStadium = new Stadium(
                null, 3L, "Wadi-Degla", "Maadi", 700.00,
                "img3.com", Type.ELEVEN_A_SIDE, 40, savedUser, null, false
        );
        stadiumRepository.save(differentLocationStadium);

        List<String> allDistinctLocations = stadiumRepository.findAllDistinctLocations();

        // Assert
        assertThat(allDistinctLocations).hasSize(2);
        assertThat(allDistinctLocations).containsExactlyInAnyOrder("Nasr-City", "Maadi");
    }
}