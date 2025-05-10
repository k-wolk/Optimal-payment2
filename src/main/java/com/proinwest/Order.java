package com.proinwest;

import java.math.BigDecimal;
import java.util.List;

public record Order(String id, BigDecimal value, List<String> promotions) {}

