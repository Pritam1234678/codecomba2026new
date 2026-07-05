package com.codecombat.challenge;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {

    @Autowired
    private TestRestTemplate rest;

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test @Order(1)
    void tc1_getAllUsersEmpty() {
        ResponseEntity<String> res = rest.getForEntity("/api/users", String.class);
        if (res.getStatusCode().value() == 200 && res.getBody() != null
                && res.getBody().trim().equals("[]")) {
            System.out.println("TC:1:PASS");
        } else {
            System.out.println("TC:1:FAIL:expected=200 []:got="
                + res.getStatusCode().value() + " " + res.getBody());
        }
    }

    @Test @Order(2)
    void tc2_createUser() {
        HttpEntity<String> req = new HttpEntity<>(
            "{\"name\":\"John Doe\",\"email\":\"john@test.com\"}", jsonHeaders());
        ResponseEntity<String> res = rest.postForEntity("/api/users", req, String.class);
        if (res.getStatusCode().value() == 201 && res.getBody() != null
                && res.getBody().contains("John Doe")) {
            System.out.println("TC:2:PASS");
        } else {
            System.out.println("TC:2:FAIL:expected=201 with John Doe:got="
                + res.getStatusCode().value() + " " + res.getBody());
        }
    }

    @Test @Order(3)
    void tc3_getUserById() {
        // First create a user to ensure id=1 exists
        HttpEntity<String> req = new HttpEntity<>(
            "{\"name\":\"Jane\",\"email\":\"jane@test.com\"}", jsonHeaders());
        rest.postForEntity("/api/users", req, String.class);

        ResponseEntity<String> res = rest.getForEntity("/api/users/1", String.class);
        if (res.getStatusCode().value() == 200 && res.getBody() != null
                && res.getBody().contains("\"id\"")) {
            System.out.println("TC:3:PASS");
        } else {
            System.out.println("TC:3:FAIL:expected=200 with user object:got="
                + res.getStatusCode().value() + " " + res.getBody());
        }
    }

    @Test @Order(4)
    void tc4_getUserNotFound() {
        ResponseEntity<String> res = rest.getForEntity("/api/users/9999", String.class);
        if (res.getStatusCode().value() == 404) {
            System.out.println("TC:4:PASS");
        } else {
            System.out.println("TC:4:FAIL:expected=404:got=" + res.getStatusCode().value());
        }
    }

    @Test @Order(5)
    void tc5_deleteUser() {
        // Create user then delete
        HttpEntity<String> req = new HttpEntity<>(
            "{\"name\":\"ToDelete\",\"email\":\"del@test.com\"}", jsonHeaders());
        ResponseEntity<String> created = rest.postForEntity("/api/users", req, String.class);
        // Extract id from response (simple parse)
        String body = created.getBody();
        String idStr = "1"; // default
        if (body != null && body.contains("\"id\":")) {
            int idx = body.indexOf("\"id\":") + 5;
            int end = body.indexOf(",", idx);
            if (end == -1) end = body.indexOf("}", idx);
            idStr = body.substring(idx, end).trim();
        }

        ResponseEntity<String> res = rest.exchange(
            "/api/users/" + idStr, HttpMethod.DELETE, null, String.class);
        if (res.getStatusCode().value() == 204) {
            System.out.println("TC:5:PASS");
        } else {
            System.out.println("TC:5:FAIL:expected=204:got=" + res.getStatusCode().value());
        }
    }

    @Test @Order(6)
    void tc6_createUserValidationFails() {
        // Empty name should fail validation
        HttpEntity<String> req = new HttpEntity<>(
            "{\"name\":\"\",\"email\":\"bad@test.com\"}", jsonHeaders());
        ResponseEntity<String> res = rest.postForEntity("/api/users", req, String.class);
        if (res.getStatusCode().value() == 400) {
            System.out.println("TC:6:PASS");
        } else {
            System.out.println("TC:6:FAIL:expected=400:got=" + res.getStatusCode().value());
        }
    }
}
