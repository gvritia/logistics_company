package com.lcorp.backend.controller;

import com.lcorp.backend.service.EmailLoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {
    private final EmailLoginService emailLogin;

    public SystemController(EmailLoginService emailLogin) {
        this.emailLogin = emailLogin;
    }

    @GetMapping("/info")
    public SystemInfo info() {
        return new SystemInfo("Logistics Company Backend", "KR3", emailLogin.enabled());
    }

    public record SystemInfo(String name, String phase, boolean emailLoginEnabled) {
    }

}
