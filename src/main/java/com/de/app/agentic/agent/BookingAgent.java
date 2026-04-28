package com.de.app.agentic.agent;

import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.BookingResult;
import com.de.app.agentic.dto.response.BudgetResult;
import com.de.app.agentic.dto.response.ItineraryResult;
import com.de.app.agentic.dto.response.ResearchResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingAgent {

    private final ChatClient chatClient;

    @Retry(name = "geminiRetry")
    @CircuitBreaker(name = "geminiCircuitBreaker")
    public BookingResult execute(TripPlanRequest request, ItineraryResult itinerary) {
        return chatClient.prompt().system("""
                You are a booking guidance agent.
                Do not claim actual bookings were made.
                Prepare booking criteria and safe next steps.
                Return structured JSON matching this schema:
                recommendedBookingSteps: string[]
                hotelSearchCriteria: string[]
                transportBookingCriteria: string[]
                attractionReservationTips: string[]
                bookingApiIntegrated: boolean
                Do not include markdown.
                """).user("""
                Request: %s
                Itinerary: %s
                """.formatted(request, itinerary)).call().entity(BookingResult.class);
    }
}
