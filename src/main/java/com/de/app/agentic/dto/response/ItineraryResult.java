package com.de.app.agentic.dto.response;

import com.de.app.agentic.dto.request.DayPlan;

import java.util.List;

public record ItineraryResult(List<DayPlan> days, List<String> highlights, List<String> warnings,
                              List<String> packingTips) {
}