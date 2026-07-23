package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.ProblemSolution;
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

    public List<ProblemSolution> getByProblem(Long problemId) {
        return repository.findByProblemIdOrderByCreatedAtDesc(problemId);
    }

    public long countByProblem(Long problemId) {
        return repository.countByProblemId(problemId);
    }
}
