package com.example.codecombat2026.entity;

import com.example.codecombat2026.util.TimeUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "problem_solutions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSolution {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name")
    private String userName;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String codes;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Map<String, String> getCodesMap() {
        if (codes == null || codes.isBlank()) return new HashMap<>();
        try { return MAPPER.readValue(codes, new TypeReference<Map<String, String>>() {}); }
        catch (Exception e) { return new HashMap<>(); }
    }

    public void setCodesMap(Map<String, String> map) {
        try { this.codes = MAPPER.writeValueAsString(map); }
        catch (Exception e) { this.codes = "{}"; }
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = TimeUtil.now();
    }
}
