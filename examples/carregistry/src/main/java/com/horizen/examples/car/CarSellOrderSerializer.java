package com.horizen.examples.car;

import com.horizen.box.BoxSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class CarSellOrderSerializer implements BoxSerializer<CarSellOrder>
{

  private static final CarSellOrderSerializer serializer;

  static {
    serializer = new CarSellOrderSerializer();
  }

  private CarSellOrderSerializer() {
    super();
  }

  public static CarSellOrderSerializer getSerializer() {
    return serializer;
  }

  @Override
  public void serialize(CarSellOrder carSellOrder, Writer writer) {
    writer.putBytes(carSellOrder.bytes());
  }

  @Override
  public CarSellOrder parse(Reader reader) {
    return CarSellOrder.parseBytes(reader.getBytes(reader.remaining()));
  }

}
