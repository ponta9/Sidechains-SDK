package com.horizen.examples.car.box;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.AbstractNoncedBox;
import com.horizen.box.BoxSerializer;
import com.horizen.examples.car.box.data.CarBoxDataSerializer;
import com.horizen.examples.car.box.data.CarBoxData;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.serialization.Views;

import java.util.Arrays;

import static com.horizen.examples.car.box.CarRegistryBoxesIdsEnum.CarBoxId;

@JsonView(Views.Default.class)
public final class CarBox extends AbstractNoncedBox<PublicKey25519Proposition, CarBoxData, CarBox> {

    public CarBox(CarBoxData boxData, long nonce) {
        super(boxData, nonce);
    }

    @Override
    public BoxSerializer serializer() {
        return CarBoxSerializer.getSerializer();
    }

    @Override
    public byte boxTypeId() {
        return CarBoxId.id();
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(
                Longs.toByteArray(nonce),
                CarBoxDataSerializer.getSerializer().toBytes(boxData)
        );
    }

    public static CarBox parseBytes(byte[] bytes) {
        long nonce = Longs.fromByteArray(Arrays.copyOf(bytes, Longs.BYTES));
        CarBoxData boxData = CarBoxDataSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, Longs.BYTES, bytes.length));

        return new CarBox(boxData, nonce);
    }

    public String getVin() {
        return boxData.getVin();
    }

    public int getYear() {
        return boxData.getYear();
    }

    public String getModel() {
        return boxData.getModel();
    }

    public String getColor() {
        return boxData.getColor();
    }

    public byte[] getCarId() {
        return Bytes.concat(
                getVin().getBytes(),
                Ints.toByteArray(getYear()),
                getModel().getBytes(),
                getColor().getBytes()
        );
    }
}
