package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.ProblemSolution;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ProblemSolutionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProblemSolutionService {

    @Autowired
    private ProblemSolutionRepository repository;

    public ProblemSolution create(Long problemId, Long userId, String userName,
                                   ProblemSolution.ProblemLanguages language,
                                   String code, String explanation, String imageUrl) {
        ProblemSolution s = new ProblemSolution();
        s.setProblemId(problemId);
        s.setUserId(userId);
        s.setUserName(userName);
        s.setLanguage(language);
        s.setCode(code);
        s.setExplanation(explanation);
        s.setImageUrl(imageUrl);
        return repository.save(s);
    }

    public ProblemSolution update(Long id, Long userId, ProblemSolution.ProblemLanguages language,
                                   String code, String explanation, String imageUrl) {
        ProblemSolution s = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Solution not found"));
        if (!s.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this solution");
        }
        s.setLanguage(language);
        s.setCode(code);
        s.setExplanation(explanation);
        s.setImageUrl(imageUrl);
        return repository.save(s);
    }

    public void delete(Long id, Long userId) {
        ProblemSolution s = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Solution not found"));
        if (!s.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this solution");
        }
        repository.delete(s);
    }

    public List<ProblemSolution> getByProblem(Long problemId) {
        return repository.findByProblemIdOrderByCreatedAtDesc(problemId);
    }

    public long countByProblem(Long problemId) {
        return repository.countByProblemId(problemId);
    }
}
