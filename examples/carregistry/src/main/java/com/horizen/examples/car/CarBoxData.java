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

//Shall we add JSON annotation for AbstractNoncedBoxData? In that case we could pass just boxData during transaction creation
//We need boxes without values!
// creation all required functions are long and take a lot of time, create a python script for it?
public class CarBoxData extends AbstractNoncedBoxData<PublicKey25519Proposition, CarBox, CarBoxData> {

  //TODO add additional attributes
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
    return CarBoxDataSerializer.getSerializer();
  }

  @Override
  public byte boxDataTypeId()
  {
    return 42;
  }

  public CarBoxData(PublicKey25519Proposition proposition, long value, BigInteger vin)
  {
    super(proposition, 1);
    this.vin = vin;
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
    return Blake2b256.hash(vin.toByteArray());
  }

  public static CarBoxData parseBytes(byte[] bytes) {
    int valueOffset = PublicKey25519Proposition.getLength();
    int activeOffset = valueOffset + Longs.BYTES;

    PublicKey25519Proposition proposition = PublicKey25519PropositionSerializer.getSerializer().parseBytes(Arrays.copyOf(bytes, valueOffset));
    long value = Longs.fromByteArray(Arrays.copyOfRange(bytes, valueOffset, activeOffset));
    BigInteger vin = new BigInteger(Arrays.copyOfRange(bytes, activeOffset, bytes.length));

    return new CarBoxData(proposition, value, vin);
  }

  @Override
  public String toString()
  {
    return "CarBoxData{" +
        "vin=" + vin + ";" +
        "proposition=" + proposition() +
        '}';
  }

  BigInteger getVin() {
    return vin;
  }
  //Shall be finals in parent?
  /*
  public int hashCode();
  public boolean equals(Object obj);*/
}
