package com.horizen.examples.car.box.data;

public enum CarRegistryBoxesDataIdsEnum {
    CarBoxDataId((byte)1),
    CarSellOrderBoxDataId((byte)2);

    private final byte id;

    CarRegistryBoxesDataIdsEnum(byte id) {
        this.id = id;
    }

    public byte id() {
        return id;
    }
}
