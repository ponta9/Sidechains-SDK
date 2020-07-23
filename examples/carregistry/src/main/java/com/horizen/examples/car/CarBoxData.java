package com.horizen.examples.car;

import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.data.AbstractNoncedBoxData;
import com.horizen.box.data.NoncedBoxDataSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.serialization.Views;
import scorex.crypto.hash.Blake2b256;

import java.math.BigInteger;
import java.util.Arrays;

//Shall we add JSON annotation for AbstractNoncedBoxData? In that case we could pass just boxData during transaction creation
//We need boxes without values!
// creation all required functions are long and take a lot of time, create a python script for it?
@JsonView(Views.Default.class)
public class CarBoxData extends AbstractNoncedBoxData<PublicKey25519Proposition, CarBox, CarBoxData> {

  private final BigInteger vin;
  private final int year;
  private final String model;
  private final String color;
  private final String description;

  @Override
  public byte[] bytes() {
    return Bytes.concat(
        proposition().bytes(),
        Longs.toByteArray(value()),
        Ints.toByteArray(year),
        Ints.toByteArray(model.getBytes().length),
        model.getBytes(),
        Ints.toByteArray(color.getBytes().length),
        color.getBytes(),
        Ints.toByteArray(description.getBytes().length),
        description.getBytes(),
        vin.toByteArray()
    );
  }

  @Override
  public NoncedBoxDataSerializer serializer()
  {
    return CarBoxDataSerializer.getSerializer();
  }

  @Override
  public byte boxDataTypeId()
  {
    return 42;
  }

  public CarBoxData(PublicKey25519Proposition proposition, long value, BigInteger vin,
                    int year, String model, String color, String description)
  {
    super(proposition, 1);
    this.vin = vin;
    this.year = year;
    this.model = model;
    this.color = color;
    this.description = description;
  }

  @Override
  public CarBox getBox(long nonce)
  {
    //shall we create a copy of data?
    return new CarBox(this, nonce);
  }

  @Override
  public byte[] customFieldsHash()
  {
    return Blake2b256.hash(
            Bytes.concat(vin.toByteArray(),
                    Ints.toByteArray(year),
                    model.getBytes(),
                    color.getBytes(),
                    description.getBytes()));
  }

  public static CarBoxData parseBytes(byte[] bytes) {
    int offset = 0;

    int l = bytes.length;

    PublicKey25519Proposition proposition = PublicKey25519PropositionSerializer.getSerializer()
            .parseBytes(Arrays.copyOf(bytes, PublicKey25519Proposition.getLength()));
    offset += PublicKey25519Proposition.getLength();

    long value = Longs.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Longs.BYTES));
    offset += Longs.BYTES;

    int year = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
    offset += Ints.BYTES;

    int size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
    offset += Ints.BYTES;

    String model = new String(Arrays.copyOfRange(bytes, offset, offset + size));
    offset += size;

    size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
    offset += Ints.BYTES;

    String color = new String(Arrays.copyOfRange(bytes, offset, offset + size));
    offset += size;

    size = Ints.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Ints.BYTES));
    offset += Ints.BYTES;

    String description = new String(Arrays.copyOfRange(bytes, offset, offset + size));
    offset += size;

    BigInteger vin = new BigInteger(Arrays.copyOfRange(bytes, offset, bytes.length));

    return new CarBoxData(proposition, value, vin, year, model, color, description);
  }

  @Override
  public String toString()
  {
    return "CarBoxData{" +
        "vin=" + vin + ";" +
        "proposition=" + proposition() +
        "model=" + model +
        "color=" + color +
        "year=" + year +
        "description=" + description +
        '}';
  }

  BigInteger getVin() {
    return vin;
  }

  public int getYear() {
    return year;
  }

  public String getModel() {
    return model;
  }

  public String getColor() {
    return color;
  }

  public String getDescription() {
    return description;
  }

  //Shall be finals in parent?
  /*
  public int hashCode();
  public boolean equals(Object obj);*/
}
