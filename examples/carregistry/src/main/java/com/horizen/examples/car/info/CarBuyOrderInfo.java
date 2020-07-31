package com.horizen.examples.car.info;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.horizen.box.data.RegularBoxData;
import com.horizen.examples.car.box.CarSellOrderBox;
import com.horizen.examples.car.box.CarSellOrderBoxSerializer;
import com.horizen.examples.car.box.data.CarBoxData;
import com.horizen.examples.car.proof.SellOrderSpendingProof;
import com.horizen.examples.car.proof.SellOrderSpendingProofSerializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.utils.BytesUtils;

import java.util.Arrays;

public final class CarBuyOrderInfo {

    private CarSellOrderBox carSellOrderBoxToOpen;
    private SellOrderSpendingProof proof;

    public CarBuyOrderInfo(CarSellOrderBox carSellOrderBoxToOpen, SellOrderSpendingProof proof) {
        this.carSellOrderBoxToOpen = carSellOrderBoxToOpen;
        this.proof = proof;
    }

    public CarSellOrderBox getCarSellOrderBoxToOpen() {
        return carSellOrderBoxToOpen;
    }

    public SellOrderSpendingProof getCarSellOrderSpendingProof() {
        return proof;
    }

    // Set new ownership to the Car
    public CarBoxData getNewOwnerCarBoxData() {
        PublicKey25519Proposition proposition;
        if(proof.isSeller()) {
            proposition = new PublicKey25519Proposition(carSellOrderBoxToOpen.proposition().getOwnerPublicKeyBytes());
        } else {
            proposition = new PublicKey25519Proposition(carSellOrderBoxToOpen.proposition().getBuyerPublicKeyBytes());
        }

        return new CarBoxData(
                proposition,
                carSellOrderBoxToOpen.getVin(),
                carSellOrderBoxToOpen.getYear(),
                carSellOrderBoxToOpen.getModel(),
                carSellOrderBoxToOpen.getColor()
        );
    }

    // Check if proof is provided by Sell order owner.
    public boolean isSpentByOwner() {
        return proof.isSeller();
    }

    // Coins to be paid to the owner of Sell order in case if Buyer spent the Sell order.
    public RegularBoxData getPaymentBoxData() {
        return new RegularBoxData(
                new PublicKey25519Proposition(carSellOrderBoxToOpen.proposition().getOwnerPublicKeyBytes()),
                carSellOrderBoxToOpen.getPrice()
        );
    }


    public byte[] bytes() {
        byte[] carSellOrderBoxToOpenBytes = CarSellOrderBoxSerializer.getSerializer().toBytes(carSellOrderBoxToOpen);
        byte[] proofBytes = SellOrderSpendingProofSerializer.getSerializer().toBytes(proof);

        return Bytes.concat(
                Ints.toByteArray(carSellOrderBoxToOpenBytes.length),
                carSellOrderBoxToOpenBytes,
                Ints.toByteArray(proofBytes.length),
                proofBytes
        );
    }

    public static CarBuyOrderInfo parseBytes(byte[] bytes) {
        int offset = 0;

        int batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        CarSellOrderBox carSellOrderBoxToOpen = CarSellOrderBoxSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        SellOrderSpendingProof proof = SellOrderSpendingProofSerializer.getSerializer().parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));

        return new CarBuyOrderInfo(carSellOrderBoxToOpen, proof);
    }
}
