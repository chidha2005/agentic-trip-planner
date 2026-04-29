package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.BudgetResult;
import com.de.app.agentic.dto.response.CriticResult;
import com.de.app.agentic.dto.response.ItineraryResult;
import com.de.app.agentic.dto.response.ResearchResult;
import com.de.app.agentic.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ItineraryAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    /**
     * Main execute method used by TripOrchestratorService
     */
    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public ItineraryResult execute(TripPlanRequest request, ResearchResult research, BudgetResult budget) {

        return buildItinerary(request, research, budget, null);
    }

    /**
     * Rework method used when CriticAgent rejects plan
     */
    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public ItineraryResult rework(TripPlanRequest request, ResearchResult research, BudgetResult budget, CriticResult critic) {

        return buildItinerary(request, research, budget, critic);
    }

    /**
     * Internal builder method
     */
    private ItineraryResult buildItinerary(TripPlanRequest request, ResearchResult research, BudgetResult budget, CriticResult critic) {

        try {
            String content = chatClient.prompt().system("""
                    You are an itinerary planning agent.
                    
                    Build a practical, optimized day-by-day trip plan.
                    
                    Return ONLY valid JSON.
                    Do not include markdown.
                    Do not include explanations.
                    Do not include YAML.
                    Do not include JavaScript object syntax.
                    All JSON field names must be enclosed in double quotes.
                    All string values must be enclosed in double quotes.
                    Response must start with { and end with }.
                    
                    Return exactly this structure:
                    
                    {
                      "days": [
                        {
                          "day": 1,
                          "cityOrArea": "string",
                          "morning": "string",
                          "afternoon": "string",
                          "evening": "string",
                          "meals": ["string"],
                          "localTransport": "string",
                          "estimatedDailyCost": "string"
                        }
                      ],
                      "highlights": ["string"],
                      "warnings": ["string"],
                      "packingTips": ["string"]
                    }
                    """).user("""
                    Destination: %s
                    Days: %d
                    Budget: %s %s
                    Travel style: %s
                    Start Date: %s
                    
                    Research Data:
                    %s
                    
                    Budget Data:
                    %s
                    
                    Critic Feedback:
                    %s
                    """.formatted(request.destination(), request.days(), request.budget(), request.currency(), request.travelStyle(), request.startDate(), research, budget, critic == null ? "None" : critic)).call().content();

            String cleaned = cleanJson(content);

            log.info("ItineraryAgent raw response={}", cleaned);

            return objectMapper.readValue(cleaned, ItineraryResult.class);

        } catch (Exception ex) {
            throw new BusinessException("ItineraryAgent failed to generate itinerary", ex);
        }
    }

    private String cleanJson(String content) {
        return content.replace("```json", "").replace("```JSON", "").replace("```", "").trim();
    }
}