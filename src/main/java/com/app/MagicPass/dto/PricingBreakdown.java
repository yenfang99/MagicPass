package com.app.MagicPass.dto;

public class PricingBreakdown {
  private double total;
  private double discount;
  private double tax;
  private double grandTotal;
  private double discountRate; // 0.05 = 5% for display purposes

  public double getTotal() { return total; }
  public void setTotal(double total) { this.total = total; }

  public double getDiscount() { return discount; }
  public void setDiscount(double discount) { this.discount = discount; }

  public double getTax() { return tax; }
  public void setTax(double tax) { this.tax = tax; }

  public double getGrandTotal() { return grandTotal; }
  public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

  public double getDiscountRate() { return discountRate; }
  public void setDiscountRate(double discountRate) { this.discountRate = discountRate; }
}
