package com.horizen.examples.car;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import com.horizen.box.data.AbstractNoncedBoxData;
import com.horizen.box.data.NoncedBoxDataSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import scorex.crypto.hash.Blake2b256;

import java.math.BigInteger;
import java.util.Arrays;

public class CarSellOrderData extends AbstractNoncedBoxData<PublicKey25519Proposition, CarSellOrder, CarSellOrderData> {
  private final BigInteger vin;

  @Override
  public byte[] bytes() {
    return Bytes.concat(
        proposition().bytes(),
        Longs.toByteArray(value()),
        vin.toByteArray()
    );
  }

  @Override
  public NoncedBoxDataSerializer serializer()
  {
    return null;
  }

  @Override
  public byte boxDataTypeId()
  {
    return 42;
  }

  public CarSellOrderData(PublicKey25519Proposition proposition, long value, BigInteger vin)
  {
    super(proposition, value);
    this.vin = vin;
  }

  @Override
  public CarSellOrder getBox(long nonce)
  {
    return new CarSellOrder(this, nonce);
  }

  @Override
  public byte[] customFieldsHash()
  {
    return Blake2b256.hash(vin.toByteArray());
  }

  public static CarSellOrderData parseBytes(byte[] bytes) {
    int valueOffset = PublicKey25519Proposition.getLength();
    int activeOffset = valueOffset + Longs.BYTES;

    PublicKey25519Proposition proposition = PublicKey25519PropositionSerializer.getSerializer().parseBytes(Arrays.copyOf(bytes, valueOffset));
    long value = Longs.fromByteArray(Arrays.copyOfRange(bytes, valueOffset, activeOffset));
    BigInteger vin = new BigInteger(Arrays.copyOfRange(bytes, activeOffset, activeOffset + Longs.BYTES));

    return new CarSellOrderData(proposition, value, vin);
  }

  @Override
  public String toString()
  {
    return "CarSellOrderData{" +
        "vin=" + vin + ";" +
        "proposition=" + proposition() +
        '}';
  }

  public BigInteger getVin() {
    return vin;
  }
}
