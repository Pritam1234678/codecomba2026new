package com.example.codecombat2026.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify that email templates exist and can be loaded.
 * 
 * Requirements: 27.2, 27.3 - Email templates with branding and unsubscribe links
 */
class EmailTemplateLoadTest {

    private String loadTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("email-templates/" + templateName + ".html");
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void testHostingRequestSubmittedTemplateExists() throws IOException {
        String html = loadTemplate("hosting-request-submitted");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{REQUEST_ID}}"));
        assertTrue(html.contains("{{USERNAME}}"));
        assertTrue(html.contains("{{DASHBOARD_URL}}"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testHostingApprovedTemplateExists() throws IOException {
        String html = loadTemplate("hosting-approved");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{FULL_NAME}}"));
        assertTrue(html.contains("{{CREATE_CONTEST_URL}}"));
        assertTrue(html.contains("Congratulations"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testHostingRejectedTemplateExists() throws IOException {
        String html = loadTemplate("hosting-rejected");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{FULL_NAME}}"));
        assertTrue(html.contains("{{REJECTION_REASON}}"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testContestCreatedTemplateExists() throws IOException {
        String html = loadTemplate("contest-created");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{CONTEST_NAME}}"));
        assertTrue(html.contains("{{INVITE_LINK}}"));
        assertTrue(html.contains("{{MANAGE_URL}}"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testFirstParticipantJoinedTemplateExists() throws IOException {
        String html = loadTemplate("first-participant-joined");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{HOST_NAME}}"));
        assertTrue(html.contains("{{PARTICIPANT_USERNAME}}"));
        assertTrue(html.contains("First Registration"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testContestStartedTemplateExists() throws IOException {
        String html = loadTemplate("contest-started");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{CONTEST_NAME}}"));
        assertTrue(html.contains("{{DASHBOARD_URL}}"));
        assertTrue(html.contains("LIVE"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testContestEndedTemplateExists() throws IOException {
        String html = loadTemplate("contest-ended");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{CONTEST_NAME}}"));
        assertTrue(html.contains("{{ANALYTICS_URL}}"));
        assertTrue(html.contains("{{SUBMISSION_COUNT}}"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testContestCancelledTemplateExists() throws IOException {
        String html = loadTemplate("contest-cancelled");
        assertNotNull(html);
        assertTrue(html.contains("CodeCoder"));
        assertTrue(html.contains("{{PARTICIPANT_NAME}}"));
        assertTrue(html.contains("{{CANCELLATION_REASON}}"));
        assertTrue(html.contains("{{HOST_NAME}}"));
        assertTrue(html.contains("unsubscribe"));
    }

    @Test
    void testAllTemplatesHaveCodeCoderLogo() throws IOException {
        String[] templates = {
            "hosting-request-submitted",
            "hosting-approved",
            "hosting-rejected",
            "contest-created",
            "first-participant-joined",
            "contest-started",
            "contest-ended",
            "contest-cancelled"
        };

        for (String templateName : templates) {
            String html = loadTemplate(templateName);
            assertTrue(html.contains("https://codecoder.in/logo.png"), 
                      "Template " + templateName + " should contain CodeCoder logo");
            assertTrue(html.contains("© 2026 CodeCoder"), 
                      "Template " + templateName + " should contain copyright footer");
        }
    }

    @Test
    void testAllTemplatesHaveUnsubscribeLink() throws IOException {
        String[] templates = {
            "hosting-request-submitted",
            "hosting-approved",
            "hosting-rejected",
            "contest-created",
            "first-participant-joined",
            "contest-started",
            "contest-ended",
            "contest-cancelled"
        };

        for (String templateName : templates) {
            String html = loadTemplate(templateName);
            assertTrue(html.contains("/unsubscribe?email="), 
                      "Template " + templateName + " should contain unsubscribe link");
        }
    }

    @Test
    void testTemplatesUseConsistentBranding() throws IOException {
        String[] templates = {
            "hosting-request-submitted",
            "hosting-approved",
            "hosting-rejected",
            "contest-created",
            "first-participant-joined",
            "contest-started",
            "contest-ended",
            "contest-cancelled"
        };

        for (String templateName : templates) {
            String html = loadTemplate(templateName);
            // Check for consistent color scheme
            assertTrue(html.contains("#f1bc8b"), 
                      "Template " + templateName + " should use brand color #f1bc8b");
            assertTrue(html.contains("#131313"), 
                      "Template " + templateName + " should use dark background #131313");
            // Check for font family
            assertTrue(html.contains("Georgia") || html.contains("Courier New"), 
                      "Template " + templateName + " should use brand typography");
        }
    }
}
