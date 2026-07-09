package com.example.codecombat2026.repository;

import com.example.codecombat2026.entity.ContestHostingRequest;
import com.example.codecombat2026.entity.ContestHostingRequest.HostingRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestHostingRequestRepository extends JpaRepository<ContestHostingRequest, Long> {
    
    List<ContestHostingRequest> findByUserId(Long userId);
    
    List<ContestHostingRequest> findByStatus(HostingRequestStatus status);
    
    Optional<ContestHostingRequest> findByUserIdAndStatus(Long userId, HostingRequestStatus status);
    
    boolean existsByUserIdAndStatus(Long userId, HostingRequestStatus status);
    
    long countByUserIdAndStatus(Long userId, HostingRequestStatus status);
}
