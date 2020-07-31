package com.horizen.examples.car.api.request;

public class CreateCarSellOrderRequest {
    public String carBoxId;
    public String buyerProposition;
    public long sellPrice;
    public long fee;

    public void setCarBoxId(String carBoxId) {
        this.carBoxId = carBoxId;
    }

    public void setBuyerProposition(String buyerProposition) {
        this.buyerProposition = buyerProposition;
    }

    public void setSellPrice(long sellPrice) {
        this.sellPrice = sellPrice;
    }

    public void setFee(int fee) {
        this.fee = fee;
    }
}
