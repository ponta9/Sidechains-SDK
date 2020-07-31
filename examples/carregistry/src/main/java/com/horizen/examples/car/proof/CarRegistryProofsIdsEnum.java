package com.horizen.examples.car.proof;

public enum CarRegistryProofsIdsEnum {
    SellOrderSpendingProofId((byte)1);

    private final byte id;

    CarRegistryProofsIdsEnum(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }
}
