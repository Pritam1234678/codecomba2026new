package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.PracticeSheet;
import com.example.codecombat2026.entity.SheetProblem;
import com.example.codecombat2026.service.SheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sheets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSheetsController {

    @Autowired private SheetService sheetService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<PracticeSheet> sheets = sheetService.getAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (PracticeSheet s : sheets) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("company", s.getCompany());
            m.put("description", s.getDescription());
            m.put("tags", s.getTags());
            m.put("active", s.getActive());
            m.put("createdAt", s.getCreatedAt());
            m.put("problemCount", sheetService.getSheetProblems(s.getId()).size());
            result.add(m);
        }
        return result;
    }

    @PostMapping
    public PracticeSheet create(@RequestBody PracticeSheet sheet) {
        return sheetService.createSheet(sheet);
    }

    @PutMapping("/{id}")
    public PracticeSheet update(@PathVariable Long id, @RequestBody PracticeSheet sheet) {
        return sheetService.updateSheet(id, sheet);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        sheetService.deleteSheet(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }

    @GetMapping("/{id}/problems")
    public List<Map<String, Object>> getProblems(@PathVariable Long id) {
        List<SheetProblem> sps = sheetService.getSheetProblems(id);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SheetProblem sp : sps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sp.getId());
            m.put("problemId", sp.getProblemId());
            m.put("sortOrder", sp.getSortOrder());
            result.add(m);
        }
        return result;
    }

    @PostMapping("/{id}/problems")
    public ResponseEntity<?> addProblems(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("problemIds");
        if (ids == null || ids.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "problemIds required"));
        List<Long> longs = ids.stream().map(Integer::longValue).toList();
        sheetService.addProblems(id, longs);
        return ResponseEntity.ok(Map.of("added", longs.size()));
    }

    @DeleteMapping("/{sheetId}/problems/{problemId}")
    public ResponseEntity<?> removeProblem(@PathVariable Long sheetId, @PathVariable Long problemId) {
        sheetService.removeProblem(sheetId, problemId);
        return ResponseEntity.ok(Map.of("removed", true));
    }
}
