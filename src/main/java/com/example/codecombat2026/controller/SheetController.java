package com.example.codecombat2026.controller;

import com.example.codecombat2026.entity.PracticeSheet;
import com.example.codecombat2026.entity.SheetProblem;
import com.example.codecombat2026.service.SheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/sheets")
@PreAuthorize("isAuthenticated()")
public class SheetController {

    @Autowired private SheetService sheetService;

    @GetMapping
    public List<PracticeSheet> list() { return sheetService.getActive(); }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSheet(@PathVariable Long id) {
        PracticeSheet sheet = sheetService.getById(id);
        List<SheetProblem> sps = sheetService.getSheetProblems(id);
        List<Long> problemIds = sps.stream().map(SheetProblem::getProblemId).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", sheet.getId());
        res.put("name", sheet.getName());
        res.put("company", sheet.getCompany());
        res.put("description", sheet.getDescription());
        res.put("tags", sheet.getTags());
        res.put("problemIds", problemIds);
        return ResponseEntity.ok(res);
    }
}
