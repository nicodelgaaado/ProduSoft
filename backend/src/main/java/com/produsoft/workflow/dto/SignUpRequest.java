package com.produsoft.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record SignUpRequest(
    @NotBlank(message = "Username is required") String username,
    @NotBlank(message = "Password is required") String password,
    @NotBlank(message = "Role is required") String role
) {
}
