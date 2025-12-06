package com.app.MagicPass.dto;

public class PricingBreakdown {
    private double total;
    private double discount;
    private double tax;
    private double grandTotal;

    public PricingBreakdown(double total, double discount, double tax, double grandTotal) {
        this.total = total;
        this.discount = discount;
        this.tax = tax;
        this.grandTotal = grandTotal;
    }

    public double getTotal() { return total; }
    public double getDiscount() { return discount; }
    public double getTax() { return tax; }
    public double getGrandTotal() { return grandTotal; }
}
