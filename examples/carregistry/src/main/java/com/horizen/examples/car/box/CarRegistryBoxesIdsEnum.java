package com.horizen.examples.car.box;

public enum CarRegistryBoxesIdsEnum {
    CarBoxId((byte)1),
    CarSellOrderBoxId((byte)2);

    private final byte id;

    CarRegistryBoxesIdsEnum(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }
}
