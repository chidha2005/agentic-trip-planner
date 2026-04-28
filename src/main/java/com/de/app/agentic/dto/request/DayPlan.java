package com.de.app.agentic.dto.request;

import java.util.List;

public record DayPlan(int day, String cityOrArea, String morning, String afternoon, String evening, List<String> meals,
                      String localTransport, String estimatedDailyCost) {
}