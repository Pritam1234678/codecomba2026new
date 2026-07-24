package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.PracticeSheet;
import com.example.codecombat2026.entity.SheetProblem;
import com.example.codecombat2026.repository.PracticeSheetRepository;
import com.example.codecombat2026.repository.SheetProblemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SheetService {

    @Autowired private PracticeSheetRepository sheetRepo;
    @Autowired private SheetProblemRepository spRepo;

    // ── Sheet CRUD ──
    public PracticeSheet createSheet(PracticeSheet sheet) { return sheetRepo.save(sheet); }
    public PracticeSheet updateSheet(Long id, PracticeSheet data) {
        PracticeSheet s = sheetRepo.findById(id).orElseThrow();
        s.setName(data.getName()); s.setCompany(data.getCompany());
        s.setDescription(data.getDescription()); s.setTags(data.getTags());
        s.setActive(data.getActive());
        return sheetRepo.save(s);
    }
    public void deleteSheet(Long id) { spRepo.deleteBySheetId(id); sheetRepo.deleteById(id); }
    public List<PracticeSheet> getAll() { return sheetRepo.findAllByOrderByCreatedAtDesc(); }
    public List<PracticeSheet> getActive() { return sheetRepo.findByActiveTrueOrderByCreatedAtDesc(); }
    public PracticeSheet getById(Long id) { return sheetRepo.findById(id).orElseThrow(); }

    // ── Problem management ──
    public void addProblem(Long sheetId, Long problemId, int sortOrder) {
        List<SheetProblem> existing = spRepo.findBySheetIdAndProblemId(sheetId, problemId);
        if (!existing.isEmpty()) return;
        SheetProblem sp = new SheetProblem();
        sp.setSheetId(sheetId); sp.setProblemId(problemId); sp.setSortOrder(sortOrder);
        spRepo.save(sp);
    }
    public void addProblems(Long sheetId, List<Long> problemIds) {
        int order = spRepo.findBySheetIdOrderBySortOrder(sheetId).size();
        for (Long pid : problemIds) {
            addProblem(sheetId, pid, order++);
        }
    }
    public void removeProblem(Long sheetId, Long problemId) {
        spRepo.deleteBySheetIdAndProblemId(sheetId, problemId);
    }
    public List<SheetProblem> getSheetProblems(Long sheetId) {
        return spRepo.findBySheetIdOrderBySortOrder(sheetId);
    }

    // ── User progress ──
    public Map<Long, Boolean> getSolvedMap(Long userId) {
        // Check which problems the user has solved (AC in practice)
        // Simplified: just return empty for now, frontend handles via practice API
        return new HashMap<>();
    }
}
