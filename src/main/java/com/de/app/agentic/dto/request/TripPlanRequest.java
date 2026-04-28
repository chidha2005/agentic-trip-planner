package com.de.app.agentic.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TripPlanRequest(@NotBlank String destination, @Min(1) @Max(30) int days,
                              @NotNull @DecimalMin("1.00") BigDecimal budget, @NotBlank String currency,
                              @NotBlank String travelStyle, @NotNull LocalDate startDate, Integer travelers) {
    public int travelerCount() {
        return travelers == null || travelers < 1 ? 1 : travelers;
    }
}