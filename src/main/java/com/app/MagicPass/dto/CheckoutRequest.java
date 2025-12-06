package com.app.MagicPass.dto;

public class CheckoutRequest {
    private int adultQty;
    private int studentQty;
    private int childQty;
    private String reservationDate; // dd/MM/yyyy

    // For now: hardcode customerId later from login session
    private String customerId = "C0003";

    public int getAdultQty() { return adultQty; }
    public void setAdultQty(int adultQty) { this.adultQty = adultQty; }
    public int getStudentQty() { return studentQty; }
    public void setStudentQty(int studentQty) { this.studentQty = studentQty; }
    public int getChildQty() { return childQty; }
    public void setChildQty(int childQty) { this.childQty = childQty; }
    public String getReservationDate() { return reservationDate; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
}...
