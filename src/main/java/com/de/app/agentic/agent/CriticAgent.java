package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CriticAgent {

    private final ChatClient chatClient;

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public CriticResult validate(TripPlanRequest request, ResearchResult research, BudgetResult budget, ItineraryResult itinerary, BookingResult booking) {
        return chatClient.prompt().system("""
                You are a validation and critic agent.
                Check the travel plan for:
                - budget issues
                - impossible timing
                - missing days
                - vague assumptions
                - unsafe claims
                - booking claims without real API execution
                Return structured JSON matching this schema:
                valid: boolean
                issues: string[]
                improvementSuggestions: string[]
                finalAssessment: string
                Do not include markdown.
                """).user("""
                Request: %s
                Research: %s
                Budget: %s
                Itinerary: %s
                Booking: %s
                """.formatted(request, research, budget, itinerary, booking)).call().entity(CriticResult.class);
    }
}
