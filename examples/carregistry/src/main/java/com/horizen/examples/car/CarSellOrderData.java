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
  private final PublicKey25519Proposition sellerProposition;
  private final BigInteger vin;

  @Override
  public byte[] bytes() {
    return Bytes.concat(
        proposition().bytes(),
        Longs.toByteArray(value()),
        sellerProposition.bytes(),
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
    return 43;
  }

  public CarSellOrderData(PublicKey25519Proposition proposition, long value, BigInteger vin, PublicKey25519Proposition sellerProposition)
  {
    super(proposition, value);
    this.vin = vin;
    this.sellerProposition = sellerProposition;
  }

  @Override
  public CarSellOrder getBox(long nonce)
  {
    return new CarSellOrder(this, nonce);
  }

  @Override
  public byte[] customFieldsHash()
  {
    return Blake2b256.hash(Bytes.concat(this.sellerProposition.pubKeyBytes(), vin.toByteArray()));
  }

  public static CarSellOrderData parseBytes(byte[] bytes) {
    int propositionLength = PublicKey25519Proposition.getLength();

    PublicKey25519Proposition proposition = PublicKey25519PropositionSerializer.getSerializer()
            .parseBytes(Arrays.copyOf(bytes, propositionLength));
    long value = Longs.fromByteArray(Arrays.copyOfRange(bytes, propositionLength, propositionLength + Longs.BYTES));

    PublicKey25519Proposition sellerProposition = PublicKey25519PropositionSerializer.getSerializer()
            .parseBytes(Arrays.copyOfRange(bytes, propositionLength + Longs.BYTES, 2*propositionLength + Longs.BYTES));

    BigInteger vin = new BigInteger(Arrays.copyOfRange(bytes, 2*propositionLength + Longs.BYTES, bytes.length));
    return new CarSellOrderData(proposition, value, vin, sellerProposition);
  }

  @Override
  public String toString()
  {
    return "CarSellOrderData{" +
        "vin=" + vin + ";" +
        "proposition=" + proposition() +
        "sellerProposition=" + proposition() +
        '}';
  }

  public BigInteger getVin() {
    return vin;
  }

  public PublicKey25519Proposition getSellerProposition() { return sellerProposition; }
}
