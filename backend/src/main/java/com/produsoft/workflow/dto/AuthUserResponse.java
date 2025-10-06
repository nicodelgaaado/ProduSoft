package com.produsoft.workflow.dto;

import java.util.List;

public record AuthUserResponse(
    String username,
    List<String> roles
) {}
