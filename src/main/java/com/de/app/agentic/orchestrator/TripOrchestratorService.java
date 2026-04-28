package com.de.app.agentic.orchestrator;

import com.de.app.agentic.agent.*;
import com.de.app.agentic.dto.AgentStatus;
import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.*;
import com.de.app.agentic.entity.TripRequestEntity;
import com.de.app.agentic.exception.BusinessException;
import com.de.app.agentic.repository.TripRequestRepository;
import com.de.app.agentic.service.AuditService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripOrchestratorService {

    private final ResearchAgent researchAgent;
    private final BudgetAgent budgetAgent;
    private final ItineraryAgent itineraryAgent;
    private final BookingAgent bookingAgent;
    private final CriticAgent criticAgent;
    private final TripRequestRepository tripRequestRepository;
    private final AuditService auditService;

    @Transactional
    public TripPlanResponse planTrip(TripPlanRequest request) {
        UUID tripRequestId = UUID.randomUUID();
        Instant now = Instant.now();

        TripRequestEntity entity = TripRequestEntity.builder().id(tripRequestId).destination(request.destination()).days(request.days()).budget(request.budget()).currency(request.currency()).travelStyle(request.travelStyle()).startDate(request.startDate()).travelers(request.travelerCount()).status("IN_PROGRESS").createdAt(now).updatedAt(now).build();

        tripRequestRepository.save(entity);

        try {
            ResearchResult research = executeResearch(tripRequestId, request);
            BudgetResult budget = executeBudget(tripRequestId, request, research);
            ItineraryResult itinerary = executeItinerary(tripRequestId, request, research, budget);
            BookingResult booking = executeBooking(tripRequestId, request, itinerary);
            CriticResult critic = executeCritic(tripRequestId, request, research, budget, itinerary, booking);

            if (!critic.valid()) {
                itinerary = executeItineraryRework(tripRequestId, request, research, budget, critic);
                booking = executeBooking(tripRequestId, request, itinerary);
                critic = executeCritic(tripRequestId, request, research, budget, itinerary, booking);
            }

            entity.setStatus(critic.valid() ? "COMPLETED" : "COMPLETED_WITH_WARNINGS");
            entity.setUpdatedAt(Instant.now());
            tripRequestRepository.save(entity);

            return new TripPlanResponse(tripRequestId, critic.valid() ? AgentStatus.SUCCESS : AgentStatus.REWORK_REQUIRED, research, budget, itinerary, booking, critic, Instant.now());
        } catch (Exception ex) {
            entity.setStatus("FAILED");
            entity.setUpdatedAt(Instant.now());
            tripRequestRepository.save(entity);
            throw new BusinessException("Trip planning failed: " + ex.getMessage(), ex);
        }
    }

    private ResearchResult executeResearch(UUID tripRequestId, TripPlanRequest request) {
        try {
            ResearchResult result = researchAgent.execute(request);
            auditService.logSuccess(tripRequestId, "ResearchAgent", request, result);
            return result;
        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "ResearchAgent", request, ex);
            throw ex;
        }
    }

    private BudgetResult executeBudget(UUID tripRequestId, TripPlanRequest request, ResearchResult research) {
        try {
            BudgetResult result = budgetAgent.execute(request, research);
            auditService.logSuccess(tripRequestId, "BudgetAgent", request, result);
            return result;
        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "BudgetAgent", request, ex);
            throw ex;
        }
    }

    private ItineraryResult executeItinerary(UUID tripRequestId, TripPlanRequest request, ResearchResult research, BudgetResult budget) {
        try {
            ItineraryResult result = itineraryAgent.execute(request, research, budget);
            auditService.logSuccess(tripRequestId, "ItineraryAgent", request, result);
            return result;
        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "ItineraryAgent", request, ex);
            throw ex;
        }
    }

    private ItineraryResult executeItineraryRework(UUID tripRequestId, TripPlanRequest request, ResearchResult research, BudgetResult budget, CriticResult critic) {
        try {
            ItineraryResult result = itineraryAgent.rework(request, research, budget, critic);
            auditService.logSuccess(tripRequestId, "ItineraryAgentRework", request, result);
            return result;
        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "ItineraryAgentRework", request, ex);
            throw ex;
        }
    }

    private BookingResult executeBooking(UUID tripRequestId, TripPlanRequest request, ItineraryResult itinerary) {
        try {
            BookingResult result = bookingAgent.execute(request, itinerary);
            auditService.logSuccess(tripRequestId, "BookingAgent", request, result);
            return result;
        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "BookingAgent", request, ex);
            throw ex;
        }
    }

    private CriticResult executeCritic(UUID tripRequestId, TripPlanRequest request, ResearchResult research, BudgetResult budget, ItineraryResult itinerary, BookingResult booking) {
        try {
            CriticResult result = criticAgent.validate(request, research, budget, itinerary, booking);
            auditService.logSuccess(tripRequestId, "CriticAgent", request, result);
            return result;
        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "CriticAgent", request, ex);
            throw ex;
        }
    }
}