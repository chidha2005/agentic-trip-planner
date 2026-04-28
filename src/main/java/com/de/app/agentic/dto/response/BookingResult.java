package com.de.app.agentic.dto.response;

import java.util.List;

public record BookingResult(List<String> recommendedBookingSteps, List<String> hotelSearchCriteria,
                            List<String> transportBookingCriteria, List<String> attractionReservationTips,
                            boolean bookingApiIntegrated) {
}