package com.de.app.agentic.dto.response;

import java.math.BigDecimal;

public record CostBreakdown(BigDecimal flights, BigDecimal hotels, BigDecimal food, BigDecimal transport,
                            BigDecimal activities, BigDecimal miscellaneous) {
}