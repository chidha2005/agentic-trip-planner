package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.ResearchResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResearchAgent {

    private final ChatClient chatClient;

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public ResearchResult execute(TripPlanRequest request) {

        return chatClient.prompt().system("""
                You are a travel research agent.
                Return only valid JSON.
                Include:
                - destination summary
                - visa notes
                - best areas to stay
                - attractions
                - transport notes
                """).user("""
                Destination: %s
                Days: %d
                Budget: %s %s
                Travel style: %s
                """.formatted(request.destination(), request.days(), request.budget(), request.currency(), request.travelStyle())).call().entity(ResearchResult.class);
    }
}