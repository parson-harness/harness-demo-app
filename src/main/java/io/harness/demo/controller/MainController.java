package io.harness.demo.controller;

import io.harness.demo.service.MetricsService;
import io.harness.demo.service.RuntimeConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final MetricsService metricsService;
    private final RuntimeConfigService runtimeConfigService;

    @GetMapping("/")
    public String home(Model model, 
                       @RequestParam(required = false) String name,
                       @RequestParam(required = false) String company) {
        
        metricsService.incrementPageViews("home");
        io.harness.demo.config.AppConfig resolvedConfig = runtimeConfigService.resolveConfig();
        
        String hostname = getHostname();
        String displayName = name != null ? name : resolvedConfig.getCustomerName();
        String displayCompany = company != null ? company : "Harness";
        
        model.addAttribute("config", resolvedConfig);
        model.addAttribute("hostname", hostname);
        model.addAttribute("userName", displayName);
        model.addAttribute("company", displayCompany);
        model.addAttribute("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        model.addAttribute("javaVersion", System.getProperty("java.version"));
        
        return "index";
    }

    @GetMapping("/services")
    public String services(Model model) {
        metricsService.incrementPageViews("services");
        model.addAttribute("config", runtimeConfigService.resolveConfig());
        return "services";
    }

    @GetMapping("/deployment")
    public String deployment(Model model) {
        metricsService.incrementPageViews("deployment");
        model.addAttribute("config", runtimeConfigService.resolveConfig());
        model.addAttribute("hostname", getHostname());
        return "deployment";
    }

    @GetMapping("/resilience")
    public String resilience(Model model) {
        metricsService.incrementPageViews("resilience");
        model.addAttribute("config", runtimeConfigService.resolveConfig());
        return "resilience";
    }

    private String getHostname() {
        try {
            String podName = System.getenv("HOSTNAME");
            if (podName != null && !podName.isEmpty()) {
                return podName;
            }
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
