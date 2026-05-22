package edu.upb.eventop.service;

import edu.upb.eventop.repository.UserRepository;
import edu.upb.eventop.repository.entity.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
@Service
public class UserService {
    private final UserRepository repository;

    @Transactional(readOnly = true)
    public Optional<User> findByUserIdToValidateSession(String id) {
        return repository.findByUserIdToValidateSession(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return repository.findByUsernameIgnoreCase(username);
    }
}
