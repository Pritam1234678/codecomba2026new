package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.ProblemComplaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemComplaintRepository extends JpaRepository<ProblemComplaint, Long> {
    List<ProblemComplaint> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ProblemComplaint> findAllByOrderByCreatedAtDesc();
    List<ProblemComplaint> findByStatusOrderByCreatedAtDesc(String status);
}
