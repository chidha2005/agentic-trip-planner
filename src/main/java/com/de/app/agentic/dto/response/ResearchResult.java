package com.de.app.agentic.dto.response;

import java.util.List;

public record ResearchResult(String destinationSummary, List<String> visaNotes, List<String> bestAreasToStay,
                             List<String> attractions, List<String> transportNotes, List<String> assumptions) {
}