package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.*;
import com.de.app.agentic.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CriticAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "geminiCircuitBreaker")
    public CriticResult validate(TripPlanRequest request, ResearchResult research, BudgetResult budget, ItineraryResult itinerary, BookingResult booking) {
        String content = chatClient.prompt().system("""
                You are a validation and critic agent.
                Check the travel plan for:
                - budget issues
                - impossible timing
                - missing days
                - vague assumptions
                - unsafe claims
                - booking claims without real API execution
                
                Return ONLY valid JSON.
                Do not include markdown.
                Do not include explanations.
                Do not include YAML.
                Do not include JavaScript object syntax.
                All JSON field names must be enclosed in double quotes.
                All string values must be enclosed in double quotes.
                Boolean values must be true or false, not strings.
                The response must start with { and end with }.
                
                Return exactly this JSON structure:
                
                {
                  "valid": true,
                  "issues": ["string"],
                  "improvementSuggestions": ["string"],
                  "finalAssessment": "string"
                }
                """).user("""
                Request: %s
                Research: %s
                Budget: %s
                Itinerary: %s
                Booking: %s
                """.formatted(request, research, budget, itinerary, booking)).call().content();

        return parse(content, CriticResult.class, "CriticAgent");
    }

    private <T> T parse(String content, Class<T> targetType, String agentName) {
        try {
            String cleaned = cleanJson(content);
            log.info("{} raw response: {}", agentName, cleaned);
            return objectMapper.readValue(cleaned, targetType);
        } catch (Exception ex) {
            throw new BusinessException(agentName + " failed to parse LLM JSON response", ex);
        }
    }

    private String cleanJson(String content) {
        return content.replace("```json", "").replace("```JSON", "").replace("```", "").trim();
    }
}
