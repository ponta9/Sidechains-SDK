package com.horizen.examples.car;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.data.AbstractNoncedBoxData;
import com.horizen.box.data.NoncedBoxDataSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import scorex.crypto.hash.Blake2b256;

import java.math.BigInteger;
import java.util.Arrays;

public class CarSellOrderData extends AbstractNoncedBoxData<PublicKey25519Proposition, CarSellOrder, CarSellOrderData> {

  private final PublicKey25519Proposition sellerProposition;
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
        sellerProposition.bytes(),
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
    return CarSellOrderDataSerializer.getSerializer();
  }

  @Override
  public byte boxDataTypeId()
  {
    return 43;
  }

  public CarSellOrderData(PublicKey25519Proposition proposition, long value, BigInteger vin,
                          PublicKey25519Proposition sellerProposition, int year, String model,
                          String color, String description)
  {
    super(proposition, value);
    this.vin = vin;
    this.sellerProposition = sellerProposition;
    this.year = year;
    this.model = model;
    this.color = color;
    this.description = description;
  }

  @Override
  public CarSellOrder getBox(long nonce)
  {
    return new CarSellOrder(this, nonce);
  }

  @Override
  public byte[] customFieldsHash()
  {
    return Blake2b256.hash(
            Bytes.concat(vin.toByteArray(),
                    sellerProposition.pubKeyBytes(),
                    Ints.toByteArray(year),
                    model.getBytes(),
                    color.getBytes(),
                    description.getBytes()));
  }

  public static CarSellOrderData parseBytes(byte[] bytes) {
    int offset = 0;

    PublicKey25519Proposition proposition = PublicKey25519PropositionSerializer.getSerializer()
            .parseBytes(Arrays.copyOf(bytes, PublicKey25519Proposition.getLength()));
    offset += PublicKey25519Proposition.getLength();

    long value = Longs.fromByteArray(Arrays.copyOfRange(bytes, offset, offset + Longs.BYTES));
    offset += Longs.BYTES;

    PublicKey25519Proposition sellerProposition = PublicKey25519PropositionSerializer.getSerializer()
            .parseBytes(Arrays.copyOfRange(bytes, offset, offset + PublicKey25519Proposition.getLength()));
    offset += PublicKey25519Proposition.getLength();

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

    return new CarSellOrderData(proposition, value, vin, sellerProposition, year, model, color, description);
  }

  @Override
  public String toString()
  {
    return "CarSellOrderData{" +
        "vin=" + vin + ";" +
        "proposition=" + proposition() +
        "sellerProposition=" + proposition() +
        "model=" + model +
        "color=" + color +
        "year=" + year +
        "description=" + description +
        '}';
  }

  public BigInteger getVin() {
    return vin;
  }

  public PublicKey25519Proposition getSellerProposition() { return sellerProposition; }

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
}
