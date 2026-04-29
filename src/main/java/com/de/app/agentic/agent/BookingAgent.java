package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.BookingResult;
import com.de.app.agentic.dto.response.BudgetResult;
import com.de.app.agentic.dto.response.ItineraryResult;
import com.de.app.agentic.dto.response.ResearchResult;
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
public class BookingAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public BookingResult execute(TripPlanRequest request, ItineraryResult itinerary) {
        String content = chatClient.prompt().system("""
                You are a booking guidance agent.
                Do not claim actual bookings were made.
                Prepare booking criteria and safe next steps.
                
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
                  "recommendedBookingSteps": ["string"],
                  "hotelSearchCriteria": ["string"],
                  "transportBookingCriteria": ["string"],
                  "attractionReservationTips": ["string"],
                  "bookingApiIntegrated": false
                }
                """).user("""
                Request: %s
                Itinerary: %s
                """.formatted(request, itinerary)).call().content();

        return parse(content, BookingResult.class, "BookingAgent");
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
