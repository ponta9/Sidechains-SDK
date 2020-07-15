package com.horizen.examples.car;

import com.horizen.box.BoxSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class CarSellOrderSerializer implements BoxSerializer<CarBox>
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
  public void serialize(CarBox box, Writer writer) {
    writer.putBytes(box.bytes());
  }

  @Override
  public CarBox parse(Reader reader) {
    return CarBox.parseBytes(reader.getBytes(reader.remaining()));
  }

}
