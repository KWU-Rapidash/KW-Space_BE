package com.example.KW_SPACE.user.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKlasId(String klasId);

	boolean existsByKlasId(String klasId);

	boolean existsByPhoneNumber(String phoneNumber);
}
