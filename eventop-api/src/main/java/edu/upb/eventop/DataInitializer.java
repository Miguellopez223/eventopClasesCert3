package edu.upb.eventop;

import edu.upb.eventop.repository.UserRepository;
import edu.upb.eventop.repository.entity.User;
import edu.upb.eventop.repository.enums.RoleType;
import edu.upb.eventop.repository.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        init();
    }

    private void init() {
        if (userRepository.count() == 0) {
            User root = userRepository.save(User.builder()
                    .username("root")
                    .email("root@upb.com")
                    .role(RoleType.ROLE_ROOT)
                    .nombres("Ricardo")
                    .apellidos("Laredo")
                    .status(UserStatus.ACTIVO)
                            .password(passwordEncoder.encode("Abc123**"))
                    .build());
        }
    }
}
