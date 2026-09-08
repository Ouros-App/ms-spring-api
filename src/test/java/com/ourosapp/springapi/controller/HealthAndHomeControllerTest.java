package com.ourosapp.springapi.controller;
import com.ourosapp.springapi.dto.address.*;
import com.ourosapp.springapi.dto.enterprise.*;
import com.ourosapp.springapi.dto.companyemployee.*;
import com.ourosapp.springapi.security.UserPrincipal;

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
