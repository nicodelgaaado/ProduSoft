package com.produsoft.workflow.controller;

import com.produsoft.workflow.dto.AuthUserResponse;
import com.produsoft.workflow.dto.SignUpRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.replaceFirst("^ROLE_", ""))
            .collect(Collectors.toList());
        return new AuthUserResponse(authentication.getName(), roles);
    }

    @PostMapping("/signup")
    public AuthUserResponse signUp(@Valid @RequestBody SignUpRequest request) {
        String username = request.username().trim();
        String normalizedRole = request.role().trim().toUpperCase(Locale.ROOT);

        if (username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        if (!normalizedRole.equals("OPERATOR") && !normalizedRole.equals("SUPERVISOR")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be OPERATOR or SUPERVISOR");
        }

        if (userDetailsManager.userExists(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        UserDetails newUser = User
            .withUsername(username)
            .password(passwordEncoder.encode(request.password()))
            .roles(normalizedRole)
            .build();

        userDetailsManager.createUser(newUser);

        return new AuthUserResponse(username, List.of(normalizedRole));
    }
}
