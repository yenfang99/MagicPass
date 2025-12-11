package com.app.MagicPass.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_code")
  private String orderCode;

  @Column(name = "user_id")
  private Long userId;

  private int adultQty;
  private int studentQty;
  private int childQty;

  private LocalDate reservationDate;

  private double total;
  private double discount;
  private double tax;
  private double grandTotal;

  private Double cashReceived;
  private Double cashChange;

  private String paymentMethod; // CASH, BANK, CARD (optional now)
  private String status;        // PREVIEW, PAID

  private LocalDateTime createdAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    if (status == null) status = "PREVIEW";
  }

  // getters/setters (generate in VS Code)
  public Long getId() { return id; }

  public String getOrderCode() { return orderCode; }
  public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }

  public int getAdultQty() { return adultQty; }
  public void setAdultQty(int adultQty) { this.adultQty = adultQty; }

  public int getStudentQty() { return studentQty; }
  public void setStudentQty(int studentQty) { this.studentQty = studentQty; }

  public int getChildQty() { return childQty; }
  public void setChildQty(int childQty) { this.childQty = childQty; }

  public LocalDate getReservationDate() { return reservationDate; }
  public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }

  public double getTotal() { return total; }
  public void setTotal(double total) { this.total = total; }

  public double getDiscount() { return discount; }
  public void setDiscount(double discount) { this.discount = discount; }

  public double getTax() { return tax; }
  public void setTax(double tax) { this.tax = tax; }

  public double getGrandTotal() { return grandTotal; }
  public void setGrandTotal(double grandTotal) { this.grandTotal = grandTotal; }

  public Double getCashReceived() { return cashReceived; }
  public void setCashReceived(Double cashReceived) { this.cashReceived = cashReceived; }

  public Double getCashChange() { return cashChange; }
  public void setCashChange(Double cashChange) { this.cashChange = cashChange; }

  public String getPaymentMethod() { return paymentMethod; }
  public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public LocalDateTime getCreatedAt() { return createdAt; }
}
