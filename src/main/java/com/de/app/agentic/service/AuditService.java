package com.de.app.agentic.service;

import com.de.app.agentic.entity.AgentExecutionLogEntity;
import com.de.app.agentic.repository.AgentExecutionLogRepository;
import com.de.app.agentic.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final JsonUtil jsonUtil;
    private final AgentExecutionLogRepository repository;

    public void logSuccess(UUID tripRequestId, String agentName, Object input, Object output) {
        repository.save(AgentExecutionLogEntity.builder().id(UUID.randomUUID()).tripRequestId(tripRequestId).agentName(agentName).inputPayload(jsonUtil.toJson(input)).outputPayload(jsonUtil.toJson(output)).status("SUCCESS").createdAt(Instant.now()).build());
    }

    public void logFailure(UUID tripRequestId, String agentName, Object input, Exception exception) {
        repository.save(AgentExecutionLogEntity.builder().id(UUID.randomUUID()).tripRequestId(tripRequestId).agentName(agentName).inputPayload(jsonUtil.toJson(input)).status("FAILED").errorMessage(exception.getMessage()).createdAt(Instant.now()).build());
    }
}