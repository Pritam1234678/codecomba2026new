package com.example.codecombat2026.service;

import com.example.codecombat2026.entity.Contest;
import com.example.codecombat2026.exception.ResourceNotFoundException;
import com.example.codecombat2026.repository.ContestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContestService {
    @Autowired
    private ContestRepository contestRepository;

    public Contest createContest(Contest contest) {
        return contestRepository.save(contest);
    }

    public Contest getContestById(Long id) {
        Contest contest = contestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contest not found with id: " + id));

        // For now, return the contest regardless of active status
        // In production, you might want to check user roles here
        return contest;
    }

    public List<Contest> getAllContests() {
        return contestRepository.findAll();
    }

    public List<Contest> getVisibleContests() {
        return contestRepository.findByActiveTrue();
    }

    public void updateContestStatus() {
        // Logic to update status based on time (scheduled task potentially)
        // For now, simple getter logic or manual update
    }
}
