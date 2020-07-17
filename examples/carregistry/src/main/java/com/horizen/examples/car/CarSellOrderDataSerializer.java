package com.horizen.examples.car;

import com.horizen.box.data.NoncedBoxDataSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public final class CarSellOrderDataSerializer implements NoncedBoxDataSerializer<CarSellOrderData>
{

  private static final CarSellOrderDataSerializer serializer = new CarSellOrderDataSerializer();

  private CarSellOrderDataSerializer() {
    super();
  }

  public static CarSellOrderDataSerializer getSerializer() {
    return serializer;
  }

  @Override
  public void serialize(CarSellOrderData carSellOrderData, Writer writer) {
    writer.putBytes(carSellOrderData.bytes());
  }

  @Override
  public CarSellOrderData parse(Reader reader) {
    return CarSellOrderData.parseBytes(reader.getBytes(reader.remaining()));
  }
}
