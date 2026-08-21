package com.ourosapp.springapi.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthAndHomeControllerTest {

    @Test
    void testHealthController() {
        HealthController healthController = new HealthController();
        Map<String, String> response = healthController.health();
        assertEquals("ok", response.get("status"));
    }

    @Test
    void testHomeController() {
        HomeController homeController = new HomeController();
        Map<String, String> response = homeController.home();
        assertTrue(response.containsKey("message"));
    }
}
