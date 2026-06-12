package com.example.codecombat2026.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static exposure is intentionally narrow: only public assets that are
        // safe to serve unauthenticated belong here. Proctoring artefacts under
        // uploads/proctoring/** (screenshots) MUST NEVER be mapped as a static
        // resource — they are served by ProctoringScreenshotController behind
        // an admin-role check (Req 14.3, 15.4, 16.1). If a new public upload
        // bucket is introduced, add it as its own explicit per-prefix handler;
        // do not regress to a broad "/uploads/**" mapping.
        registry.addResourceHandler("/uploads/profile-photos/**")
                .addResourceLocations("file:uploads/profile-photos/");
    }
}
