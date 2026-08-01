package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.ProblemComplaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemComplaintRepository extends JpaRepository<ProblemComplaint, Long> {
    Page<ProblemComplaint> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<ProblemComplaint> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ProblemComplaint> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
