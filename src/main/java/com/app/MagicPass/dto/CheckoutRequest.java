package com.app.MagicPass.dto;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class CheckoutRequest {
    private int adultQty;
    private int studentQty;
    private int childQty;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) // expects yyyy-MM-dd from <input type="date">
    private LocalDate reservationDate;

    private String customerId = "C0003";

    public int getAdultQty() { return adultQty; }
    public void setAdultQty(int adultQty) { this.adultQty = adultQty; }
    public int getStudentQty() { return studentQty; }
    public void setStudentQty(int studentQty) { this.studentQty = studentQty; }
    public int getChildQty() { return childQty; }
    public void setChildQty(int childQty) { this.childQty = childQty; }

    public LocalDate getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDate reservationDate) { this.reservationDate = reservationDate; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}
