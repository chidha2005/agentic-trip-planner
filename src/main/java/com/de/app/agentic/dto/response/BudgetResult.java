package com.de.app.agentic.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record BudgetResult(BigDecimal estimatedTotal, BigDecimal remainingBudget, String currency, boolean withinBudget,
                           CostBreakdown costBreakdown, List<String> costSavingTips, List<String> assumptions) {
}