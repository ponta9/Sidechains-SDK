package com.horizen.examples.car.info;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.examples.car.box.CarBox;
import com.horizen.examples.car.box.CarBoxSerializer;
import com.horizen.examples.car.box.data.CarSellOrderBoxData;
import com.horizen.examples.car.proposition.SellOrderProposition;
import com.horizen.proof.Signature25519;
import com.horizen.proof.Signature25519Serializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.utils.BytesUtils;

import java.util.Arrays;

public final class CarSellOrderInfo {

    private CarBox carBoxToOpen;
    private Signature25519 proof;
    private long price;
    private PublicKey25519Proposition buyerProposition;

    public CarSellOrderInfo(CarBox carBoxToOpen, Signature25519 proof, long price, PublicKey25519Proposition buyerProposition) {
        this.carBoxToOpen = carBoxToOpen;
        this.proof = proof;
        this.price = price;
        this.buyerProposition = buyerProposition;
    }

    public CarBox getCarBoxToOpen() {
        return carBoxToOpen;
    }

    public Signature25519 getCarBoxSpendingProof() {
        return proof;
    }

    public CarSellOrderBoxData getSellOrderBoxData() {
        return new CarSellOrderBoxData(
                new SellOrderProposition(carBoxToOpen.proposition().pubKeyBytes(), buyerProposition.pubKeyBytes()),
                price,
                carBoxToOpen.getVin(),
                carBoxToOpen.getYear(),
                carBoxToOpen.getModel(),
                carBoxToOpen.getColor()
        );
    }

    public byte[] bytes() {
        byte[] carBoxToOpenBytes = CarBoxSerializer.getSerializer().toBytes(carBoxToOpen);
        byte[] proofBytes = Signature25519Serializer.getSerializer().toBytes(proof);

        byte[] buyerPropositionBytes = PublicKey25519PropositionSerializer.getSerializer().toBytes(buyerProposition);

        return Bytes.concat(
                Ints.toByteArray(carBoxToOpenBytes.length),
                carBoxToOpenBytes,
                Ints.toByteArray(proofBytes.length),
                proofBytes,
                Longs.toByteArray(price),
                Ints.toByteArray(buyerPropositionBytes.length),
                buyerPropositionBytes
        );
    }

    public static CarSellOrderInfo parseBytes(byte[] bytes) {
        int offset = 0;

        int batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        CarBox carBoxToOpen = CarBoxSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        Signature25519 proof = Signature25519Serializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        long price = BytesUtils.getLong(bytes, offset);
        offset += 8;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        PublicKey25519Proposition buyerProposition = PublicKey25519PropositionSerializer.getSerializer()
                .parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));

        return new CarSellOrderInfo(carBoxToOpen, proof, price, buyerProposition);
    }
}
