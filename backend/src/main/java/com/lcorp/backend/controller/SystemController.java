package com.lcorp.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Небольшой REST-маршрут подтверждает, что web-слой подключён к приложению.
@RestController
@RequestMapping("/api/system")
public class SystemController {
    @GetMapping("/info")
    public SystemInfo info() {
        return new SystemInfo("Logistics Company Backend", "KR3");
    }

    public record SystemInfo(String name, String phase) {
    }
}
