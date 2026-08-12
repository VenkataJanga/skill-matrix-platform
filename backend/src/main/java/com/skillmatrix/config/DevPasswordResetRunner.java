package com.skillmatrix.config;

import com.skillmatrix.domain.entity.User;
import com.skillmatrix.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * DEV-ONLY: Resets all demo user passwords on startup using the live PasswordEncoder.
 *
 * <p>This guarantees the BCrypt hash in the DB always matches what the running
 * PasswordEncoder expects, regardless of how or when the seed data was generated.
 *
 * <p>Only active when {@code spring.profiles.active=dev}.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevPasswordResetRunner implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Password@123";
    private static final List<String> DEMO_USERNAMES = List.of(
            "admin",
            "dumitru_baboiu", "thomas_daniel", "frank_going", "venkata_janga",
            "deepak_mishra", "waseem_mp", "bhupendra_singh",
            // Legacy names kept as fallback in case V07 hasn't run yet
            "lead_manager", "tech_john", "tech_jane", "tech_mike"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String freshHash = passwordEncoder.encode(DEMO_PASSWORD);
        int updated = 0;

        for (String username : DEMO_USERNAMES) {
            Optional<User> opt = userRepository.findByUsername(username);
            if (opt.isPresent()) {
                User user = opt.get();
                user.setPasswordHash(freshHash);
                user.setFailedLoginAttempts(0);
                user.setAccountLocked(false);
                user.setLockTime(null);
                userRepository.save(user);
                updated++;
            }
        }

        log.info("[DEV] DevPasswordResetRunner: reset passwords for {} demo user(s). Use 'Password@123' to login.",
                updated);
    }
}
