package com.de.app.agentic.memory;

import com.de.app.agentic.repository.AgentExecutionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MemoryService {

    private final AgentExecutionLogRepository repository;

    public List<String> getExecutionHistory(UUID tripRequestId) {
        return repository.findByTripRequestIdOrderByCreatedAtAsc(tripRequestId).stream().map(log -> log.getAgentName() + ": " + log.getStatus()).toList();
    }
}
