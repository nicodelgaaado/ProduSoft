package com.produsoft.workflow.controller;

import com.produsoft.workflow.dto.AuthUserResponse;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .map(role -> role.replaceFirst("^ROLE_", ""))
            .collect(Collectors.toList());
        return new AuthUserResponse(authentication.getName(), roles);
    }
}
