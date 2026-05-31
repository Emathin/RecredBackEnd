package com.recred.backend.domain.services;

import com.recred.backend.domain.models.User;
import com.recred.backend.domain.repositories.UserRepository;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User synchronizeCognitoUser(Jwt jwt) {
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");

        String name = jwt.getClaimAsString("given_name");
        String surname = jwt.getClaimAsString("family_name");

        Optional<User> existingUser = userRepository.findByIdCognito(sub);

        if (existingUser.isPresent()) {
            return existingUser.get();
        }

        User newUser = new User();

        newUser.setId(UUID.randomUUID());

        newUser.setIdCognito(sub);
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setSurname(surname);

        newUser.setCheckedByCognito(true);
        newUser.setActive(true);
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());

        return userRepository.save(newUser);
    }

    public User getCurrentUSer(String idCognito) {
        return userRepository.findByIdCognito(idCognito)
                .orElseThrow(() -> new RuntimeException("Usuario no " +
                        "encontrado en la base de datos"));
    }
}