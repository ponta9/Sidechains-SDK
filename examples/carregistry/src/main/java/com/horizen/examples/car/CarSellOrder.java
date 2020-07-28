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
public class CarSellOrder extends AbstractNoncedBox<PublicKey25519Proposition, CarSellOrderData, CarSellOrder> {

  public CarSellOrder(CarSellOrderData boxData, long nonce) {
    super(boxData, nonce);
  }

  @Override
  public byte[] bytes()
  {
    return Bytes.concat(
        Longs.toByteArray(nonce),
        CarSellOrderDataSerializer.getSerializer().toBytes(boxData)
    );
  }

  @Override
  public BoxSerializer serializer()
  {
    return CarSellOrderSerializer.getSerializer();
  }

  @Override
  public byte boxTypeId()
  {
    return 43;
  }

  public static CarSellOrder parseBytes(byte[] bytes) {
    long nonce = Longs.fromByteArray(Arrays.copyOf(bytes, Longs.BYTES));
    CarSellOrderData boxData = CarSellOrderDataSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, Longs.BYTES, bytes.length));

    return new CarSellOrder(boxData, nonce);
  }

  public CarSellOrderData getBoxData() {
    return this.boxData;
  }
}
