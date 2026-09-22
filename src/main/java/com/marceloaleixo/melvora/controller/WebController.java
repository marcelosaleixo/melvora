package com.marceloaleixo.melvora.controller;

import com.marceloaleixo.melvora.service.DashboardService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private final DashboardService dashboardService;

    public WebController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/login")
    public String login() {
        return "pages/auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("dashboard", dashboardService.carregar(authentication.getName()));
        return "pages/dashboard";
    }
}
