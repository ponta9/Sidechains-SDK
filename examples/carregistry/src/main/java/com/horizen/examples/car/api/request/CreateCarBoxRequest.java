package com.horizen.examples.car.api.request;

public class CreateCarBoxRequest {
    public String vin;
    public int year;
    public String model;
    public String color;
    public String proposition; // hex representation of public key proposition
    public long fee;

    public void setVin(String vin) {
        this.vin = vin;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setProposition(String proposition) {
        this.proposition = proposition;
    }

    public void setFee(long fee) {
        this.fee = fee;
    }
}
