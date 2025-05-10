package com.proinwest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Usage {
    private BigDecimal total = BigDecimal.ZERO;

    public void add(BigDecimal amount) {
        total = total.add(amount);
    }

    public BigDecimal getTotal() {
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
