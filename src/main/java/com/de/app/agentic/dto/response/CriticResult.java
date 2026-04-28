package com.de.app.agentic.dto.response;

import java.util.List;

public record CriticResult(boolean valid, List<String> issues, List<String> improvementSuggestions,
                           String finalAssessment) {
}