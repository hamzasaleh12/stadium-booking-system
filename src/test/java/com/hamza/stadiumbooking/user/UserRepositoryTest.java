package com.hamza.stadiumbooking.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;
    private User savedUser;
    @BeforeEach
    void setUp(){
        userRepository.deleteAll();
        User user = new User(
                null, 0L, "hamza", "hamza@gmail.com", "01000000000",
                "12345", LocalDate.now(), Role.ROLE_PLAYER, null, false
        );
        savedUser = userRepository.save(user);
    }


    @Test
    void ShouldToBeFindByEmail() {
        Optional<User> expected = userRepository.findByEmailAndIsDeletedFalse(savedUser.getEmail());


        assertThat(expected).isPresent();
        assertThat(expected.get().getEmail()).isEqualTo(savedUser.getEmail());
    }
    @Test
    void ShouldNotFindByEmail_IsDeletedTrue() {
        savedUser.setDeleted(true);
        userRepository.save(savedUser);
        Optional<User> expected = userRepository.findByEmailAndIsDeletedFalse(savedUser.getEmail());

        assertThat(expected).isEmpty();
    }
    @Test
    void ShouldNotFindByEmail() {
        Optional<User> expected = userRepository.findByEmailAndIsDeletedFalse("fake@gmail.com");

        assertThat(expected).isEmpty();
    }

    @Test
    void shouldBeExistByIdAndEmail(){
        boolean test = userRepository.existsByIdAndEmail(savedUser.getId(),savedUser.getEmail());

        assertThat(test).isTrue();
    }
    @Test
    void shouldNotBeExistByIdAndEmail(){
        boolean test = userRepository.existsByIdAndEmail(savedUser.getId(),"fake@gmail.com");

        assertThat(test).isFalse();
    }

    @Test
    void getUserIdByEmail(){
        Long id = userRepository.getUserIdByEmail(savedUser.getEmail());

        assertThat(id).isNotNull();
        assertThat(id).isEqualTo(savedUser.getId());
    }
    @Test
    void shouldNotGetUserIdByEmail(){
        Long id = userRepository.getUserIdByEmail("fake@gmail.com");

        assertThat(id).isNull();
    }

    @Test
    void findByIdAndIsDeletedFalse(){
        Optional<User> user = userRepository.findByIdAndIsDeletedFalse(savedUser.getId());

        assertThat(user).isNotNull();
        assertThat(user.get().getId()).isEqualTo(savedUser.getId());
    }
    @Test
    void findByIdAndIsDeletedTrue(){
        savedUser.setDeleted(true);
        userRepository.save(savedUser);

        Optional<User> user = userRepository.findByIdAndIsDeletedFalse(savedUser.getId());

        assertThat(user).isEmpty();
    }
    @Test
    void findByIdAndIsDeletedFalse_ShouldNotFound(){
        Optional<User> user = userRepository.findByIdAndIsDeletedFalse(999L);

        assertThat(user).isEmpty();
    }

    @Test
    void findAllByIsDeletedFalse(){
        Page<User> user = userRepository.findAllByIsDeletedFalse(Pageable.unpaged());

        assertThat(user).isNotEmpty();
        assertThat(user.getContent().getFirst().getId()).isEqualTo(savedUser.getId());
    }
    @Test
    void findAllByIsDeletedTrue(){
        savedUser.setDeleted(true);
        userRepository.save(savedUser);

        Page<User> user = userRepository.findAllByIsDeletedFalse(Pageable.unpaged());

        assertThat(user).isEmpty();
    }
    @Test
    void findAllByIsDeletedFalse_ShouldNotFound(){
        userRepository.deleteAll();
        Page<User> user = userRepository.findAllByIsDeletedFalse(Pageable.unpaged());

        assertThat(user).isEmpty();
    }
}