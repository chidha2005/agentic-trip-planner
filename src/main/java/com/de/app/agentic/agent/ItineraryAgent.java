package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.BudgetResult;
import com.de.app.agentic.dto.response.CriticResult;
import com.de.app.agentic.dto.response.ItineraryResult;
import com.de.app.agentic.dto.response.ResearchResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItineraryAgent {

    private final ChatClient chatClient;

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public ItineraryResult execute(TripPlanRequest request, ResearchResult research, BudgetResult budget) {
        return buildItinerary(request, research, budget, null);
    }

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public ItineraryResult rework(TripPlanRequest request, ResearchResult research, BudgetResult budget, CriticResult critic) {
        return buildItinerary(request, research, budget, critic);
    }

    private ItineraryResult buildItinerary(TripPlanRequest request, ResearchResult research, BudgetResult budget, CriticResult critic) {
        return chatClient.prompt().system("""
                You are an itinerary planning agent.
                Build a practical day-by-day travel plan.
                Return structured JSON matching this schema:
                days: [
                  {
                    day: number,
                    cityOrArea: string,
                    morning: string,
                    afternoon: string,
                    evening: string,
                    meals: string[],
                    localTransport: string,
                    estimatedDailyCost: string
                  }
                ]
                highlights: string[]
                warnings: string[]
                packingTips: string[]
                Do not include markdown.
                """).user("""
                Request: %s
                Research: %s
                Budget: %s
                Critic feedback if any: %s
                """.formatted(request, research, budget, critic)).call().entity(ItineraryResult.class);
    }
}
