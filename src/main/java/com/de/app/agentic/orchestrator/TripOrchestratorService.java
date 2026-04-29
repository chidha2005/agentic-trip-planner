package com.de.app.agentic.orchestrator;

import com.de.app.agentic.agent.BookingAgent;
import com.de.app.agentic.agent.BudgetAgent;
import com.de.app.agentic.agent.CriticAgent;
import com.de.app.agentic.agent.ItineraryAgent;
import com.de.app.agentic.agent.ResearchAgent;
import com.de.app.agentic.dto.AgentStatus;
import com.de.app.agentic.dto.request.TripPlanRequest;
import com.de.app.agentic.dto.response.*;
import com.de.app.agentic.entity.TripRequestEntity;
import com.de.app.agentic.exception.BusinessException;
import com.de.app.agentic.repository.TripRequestRepository;
import com.de.app.agentic.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            log.info("Trip planning started. tripRequestId={}, destination={}", tripRequestId, request.destination());

            ResearchResult research = executeResearch(tripRequestId, request);

            BudgetResult budget = executeBudget(tripRequestId, request, research);

            ItineraryResult itinerary = executeItinerary(tripRequestId, request, research, budget);

            BookingResult booking = executeBooking(tripRequestId, request, itinerary);

            CriticResult critic = executeCritic(tripRequestId, request, research, budget, itinerary, booking);

            if (!critic.valid()) {
                log.warn("CriticAgent requested itinerary rework. tripRequestId={}, issues={}", tripRequestId, critic.issues());

                itinerary = executeItineraryRework(tripRequestId, request, research, budget, critic);

                booking = executeBooking(tripRequestId, request, itinerary);

                critic = executeCritic(tripRequestId, request, research, budget, itinerary, booking);
            }

            AgentStatus finalStatus = critic.valid() ? AgentStatus.SUCCESS : AgentStatus.REWORK_REQUIRED;

            entity.setStatus(critic.valid() ? "COMPLETED" : "COMPLETED_WITH_WARNINGS");
            entity.setUpdatedAt(Instant.now());
            tripRequestRepository.save(entity);

            log.info("Trip planning completed. tripRequestId={}, status={}", tripRequestId, finalStatus);

            return new TripPlanResponse(tripRequestId, finalStatus, research, budget, itinerary, booking, critic, Instant.now());

        } catch (Exception ex) {
            log.error("Trip planning failed. tripRequestId={}", tripRequestId, ex);

            entity.setStatus("FAILED");
            entity.setUpdatedAt(Instant.now());
            tripRequestRepository.save(entity);

            throw new BusinessException("Trip planning failed: " + ex.getMessage(), ex);
        }
    }

    private ResearchResult executeResearch(UUID tripRequestId, TripPlanRequest request) {
        try {
            log.info("ResearchAgent started. tripRequestId={}", tripRequestId);

            ResearchResult result = researchAgent.execute(request);

            auditService.logSuccess(tripRequestId, "ResearchAgent", request, result);

            log.info("ResearchAgent completed. tripRequestId={}", tripRequestId);
            return result;

        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "ResearchAgent", request, ex);
            throw ex;
        }
    }

    private BudgetResult executeBudget(UUID tripRequestId, TripPlanRequest request, ResearchResult research) {
        try {
            log.info("BudgetAgent started. tripRequestId={}", tripRequestId);

            BudgetResult result = budgetAgent.execute(request, research);

            auditService.logSuccess(tripRequestId, "BudgetAgent", request, result);

            log.info("BudgetAgent completed. tripRequestId={}", tripRequestId);
            return result;

        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "BudgetAgent", request, ex);
            throw ex;
        }
    }

    private ItineraryResult executeItinerary(UUID tripRequestId, TripPlanRequest request, ResearchResult research, BudgetResult budget) {
        try {
            log.info("ItineraryAgent started. tripRequestId={}", tripRequestId);

            ItineraryResult result = itineraryAgent.execute(request, research, budget);

            auditService.logSuccess(tripRequestId, "ItineraryAgent", request, result);

            log.info("ItineraryAgent completed. tripRequestId={}", tripRequestId);
            return result;

        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "ItineraryAgent", request, ex);
            throw ex;
        }
    }

    private ItineraryResult executeItineraryRework(UUID tripRequestId, TripPlanRequest request, ResearchResult research, BudgetResult budget, CriticResult critic) {
        try {
            log.info("ItineraryAgent rework started. tripRequestId={}", tripRequestId);

            ItineraryResult result = itineraryAgent.rework(request, research, budget, critic);

            auditService.logSuccess(tripRequestId, "ItineraryAgentRework", request, result);

            log.info("ItineraryAgent rework completed. tripRequestId={}", tripRequestId);
            return result;

        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "ItineraryAgentRework", request, ex);
            throw ex;
        }
    }

    private BookingResult executeBooking(UUID tripRequestId, TripPlanRequest request, ItineraryResult itinerary) {
        try {
            log.info("BookingAgent started. tripRequestId={}", tripRequestId);

            BookingResult result = bookingAgent.execute(request, itinerary);

            auditService.logSuccess(tripRequestId, "BookingAgent", request, result);

            log.info("BookingAgent completed. tripRequestId={}", tripRequestId);
            return result;

        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "BookingAgent", request, ex);
            throw ex;
        }
    }

    private CriticResult executeCritic(UUID tripRequestId, TripPlanRequest request, ResearchResult research, BudgetResult budget, ItineraryResult itinerary, BookingResult booking) {
        try {
            log.info("CriticAgent started. tripRequestId={}", tripRequestId);

            CriticResult result = criticAgent.validate(request, research, budget, itinerary, booking);

            auditService.logSuccess(tripRequestId, "CriticAgent", request, result);

            log.info("CriticAgent completed. tripRequestId={}, valid={}", tripRequestId, result.valid());

            return result;

        } catch (Exception ex) {
            auditService.logFailure(tripRequestId, "CriticAgent", request, ex);
            throw ex;
        }
    }
}