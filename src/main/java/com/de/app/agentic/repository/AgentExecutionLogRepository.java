package com.de.app.agentic.repository;

import com.de.app.agentic.entity.AgentExecutionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentExecutionLogRepository extends JpaRepository<AgentExecutionLogEntity, UUID> {
    List<AgentExecutionLogEntity> findByTripRequestIdOrderByCreatedAtAsc(UUID tripRequestId);
}