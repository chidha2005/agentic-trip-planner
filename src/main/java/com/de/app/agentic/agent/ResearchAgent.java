package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
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
public class ResearchAgent {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public ResearchResult execute(TripPlanRequest request) {

        String content = chatClient.prompt().system("""
                You are a travel research agent.
                
                Return ONLY valid JSON.
                Do not include markdown.
                Do not include explanations.
                Do not include YAML.
                Do not include JavaScript object syntax.
                All JSON field names must be enclosed in double quotes.
                All string values must be enclosed in double quotes.
                The response must start with { and end with }.
                
                Return exactly this JSON structure:
                
                {
                  "destinationSummary": "string",
                  "visaNotes": ["string"],
                  "bestAreasToStay": ["string"],
                  "attractions": ["string"],
                  "transportNotes": ["string"],
                  "assumptions": ["string"]
                }
                """).user("""
                Destination: %s
                Days: %d
                Budget: %s %s
                Travel style: %s
                Start date: %s
                Travelers: %d
                """.formatted(request.destination(), request.days(), request.budget(), request.currency(), request.travelStyle(), request.startDate(), request.travelerCount())).call().content();

        return parse(content, ResearchResult.class, "ResearchAgent");
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