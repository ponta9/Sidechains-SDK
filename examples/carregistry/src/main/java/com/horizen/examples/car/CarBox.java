package com.horizen.examples.car;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.horizen.box.AbstractNoncedBox;
import com.horizen.box.BoxSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.serialization.Views;

import java.util.Arrays;

@JsonView(Views.Default.class)
public class CarBox extends AbstractNoncedBox<PublicKey25519Proposition, CarBoxData, CarBox> {

  public CarBox(CarBoxData boxData, long nonce) {
    super(boxData, nonce);
  }

  //shall be default final implementation?
  @Override
  public byte[] bytes()
  {
    return Bytes.concat(
        Longs.toByteArray(nonce),
        CarBoxDataSerializer.getSerializer().toBytes(boxData)
    );
  }

  @Override
  public BoxSerializer serializer()
  {
    return CarBoxSerializer.getSerializer();
  }

  //use data type from BoxData?
  @Override
  public byte boxTypeId()
  {
    return 42;
  }

  public static CarBox parseBytes(byte[] bytes) {
    long nonce = Longs.fromByteArray(Arrays.copyOf(bytes, Longs.BYTES));
    CarBoxData boxData = CarBoxDataSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, Longs.BYTES, bytes.length));

    return new CarBox(boxData, nonce);
  }

  public CarBoxData getBoxData() {
    return boxData;
  }
}
