package com.proinwest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class OptimalPaymentSolver {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        List<Order> orders = mapper.readValue(new File(args[0]), new TypeReference<>() {});
        List<PaymentMethod> methods = mapper.readValue(new File(args[1]), new TypeReference<>() {});

        Map<String, PaymentMethod> methodMap = new HashMap<>();
        for (PaymentMethod pm : methods) {
            methodMap.put(pm.getId(), pm);
        }

        Map<String, Usage> usageMap = new HashMap<>();
        for (PaymentMethod pm : methods) {
            usageMap.put(pm.getId(), new Usage());
        }

        for (Order order : orders) {
            BigDecimal value = order.value();
            BigDecimal bestCost = value;
            String bestMethod = null;
            boolean usePoints = false;
            BigDecimal pointsAmount = BigDecimal.ZERO;

            // 1. Full payment by one eligible method (max discount)
            if (order.promotions() != null) {
                for (String methodId : order.promotions()) {
                    PaymentMethod method = methodMap.get(methodId);
                    if (method != null) {
                        BigDecimal discountAmount = value.multiply(BigDecimal.valueOf(method.getDiscount()))
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        BigDecimal cost = value.subtract(discountAmount);
                        if (method.getLimit().compareTo(cost) >= 0 && cost.compareTo(bestCost) < 0) {
                            bestCost = cost;
                            bestMethod = methodId;
                            usePoints = false;
                        }
                    }
                }
            }

            // 2. Full payment by points
            PaymentMethod points = methodMap.get("PUNKTY");
            if (points != null && points.getLimit().compareTo(value) >= 0) {
                BigDecimal discount = value.multiply(BigDecimal.valueOf(points.getDiscount()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal cost = value.subtract(discount);
                if (cost.compareTo(bestCost) < 0) {
                    bestCost = cost;
                    bestMethod = "PUNKTY";
                    usePoints = false;
                }
            }

            // 3. Partial points (>=10%) + traditional method (no promotion discount, only 10% overall discount)
            BigDecimal tenPercent = value.multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
            if (points != null && points.getLimit().compareTo(tenPercent) >= 0) {
                BigDecimal maxPointUse = points.getLimit().min(value);
                BigDecimal rest = value.subtract(maxPointUse);
                BigDecimal totalAfterDiscount = value.multiply(BigDecimal.valueOf(0.90)).setScale(2, RoundingMode.HALF_UP);
                for (PaymentMethod method : methods) {
                    if (!method.getId().equals("PUNKTY") && (order.promotions() == null || !order.promotions().contains(method.getId()))) {
                        if (method.getLimit().compareTo(rest) >= 0) {
                            if (totalAfterDiscount.compareTo(bestCost) < 0) {
                                bestCost = totalAfterDiscount;
                                bestMethod = method.getId();
                                usePoints = true;
                                pointsAmount = maxPointUse;
                            }
                        }
                    }
                }
            }

            // Apply payment
            if (usePoints) {
                PaymentMethod method = methodMap.get(bestMethod);
                BigDecimal finalValue = value.multiply(BigDecimal.valueOf(0.90)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal restPay = finalValue.subtract(pointsAmount);

                usageMap.get("PUNKTY").add(pointsAmount);
                points.setLimit(points.getLimit().subtract(pointsAmount));

                usageMap.get(bestMethod).add(restPay);
                method.setLimit(method.getLimit().subtract(restPay));

            } else if (bestMethod != null) {
                BigDecimal discount = value.multiply(BigDecimal.valueOf(methodMap.get(bestMethod).getDiscount())).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal toPay = value.subtract(discount);
                usageMap.get(bestMethod).add(toPay);
                methodMap.get(bestMethod).setLimit(methodMap.get(bestMethod).getLimit().subtract(toPay));
            }
        }

        // Print result
        for (Map.Entry<String, Usage> entry : usageMap.entrySet()) {
            if (entry.getValue().getTotal().compareTo(BigDecimal.ZERO) > 0) {
                System.out.println(entry.getKey() + " " + entry.getValue().getTotal());
            }
        }
    }
}
