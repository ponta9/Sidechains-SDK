package com.horizen.examples.car;

import com.horizen.box.BoxSerializer;
import com.horizen.box.CertifierRightBox;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public class CarBoxSerializer implements BoxSerializer<CarBox>
{

  private static final CarBoxSerializer serializer;

  static {
    serializer = new CarBoxSerializer();
  }

  private CarBoxSerializer() {
    super();
  }

  public static CarBoxSerializer getSerializer() {
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
