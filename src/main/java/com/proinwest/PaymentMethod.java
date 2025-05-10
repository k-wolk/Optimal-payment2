package com.proinwest;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentMethod {
    public String id;
    public int discount;
    public BigDecimal limit;
}
