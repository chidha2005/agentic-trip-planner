package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.BudgetResult;
import com.de.app.agentic.dto.response.ResearchResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BudgetAgent {

    private final ChatClient chatClient;

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public BudgetResult execute(TripPlanRequest request, ResearchResult researchResult) {
        return chatClient.prompt().system("""
                You are a budget planning agent.
                Estimate trip costs realistically.
                Return structured JSON matching this schema:
                estimatedTotal: number
                remainingBudget: number
                currency: string
                withinBudget: boolean
                costBreakdown: {
                  flights: number,
                  hotels: number,
                  food: number,
                  transport: number,
                  activities: number,
                  miscellaneous: number
                }
                costSavingTips: string[]
                assumptions: string[]
                Do not include markdown.
                """).user("""
                Request: %s
                Research: %s
                """.formatted(request, researchResult)).call().entity(BudgetResult.class);
    }
}
