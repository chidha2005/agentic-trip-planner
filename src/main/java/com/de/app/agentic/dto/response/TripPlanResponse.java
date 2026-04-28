package com.de.app.agentic.dto.response;

import com.de.app.agentic.dto.AgentStatus;

import java.time.Instant;
import java.util.UUID;

public record TripPlanResponse(UUID tripRequestId, AgentStatus status, ResearchResult research, BudgetResult budget,
                               ItineraryResult itinerary, BookingResult booking, CriticResult critic,
                               Instant generatedAt) {
}