package com.horizen.examples.car;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.horizen.box.BoxUnlocker;
import com.horizen.box.NoncedBox;
import com.horizen.box.data.NoncedBoxData;
import com.horizen.box.data.RegularBoxData;
import com.horizen.companion.SidechainBoxesCompanion;
import com.horizen.companion.SidechainBoxesDataCompanion;
import com.horizen.companion.SidechainProofsCompanion;
import com.horizen.examples.car.box.data.CarBoxData;
import com.horizen.examples.car.box.data.CarSellOrderBoxData;
import com.horizen.proof.Proof;
import com.horizen.proposition.Proposition;
import com.horizen.transaction.SidechainTransaction;
import com.horizen.transaction.TransactionSerializer;
import com.horizen.utils.BytesUtils;
import com.horizen.utils.ListSerializer;
import scorex.core.NodeViewModifier$;

import java.io.ByteArrayOutputStream;
import java.util.*;


/*public class CarSellTransaction
        extends SidechainTransaction<Proposition, NoncedBox<Proposition>>
{
    private List<byte[]> inputsIds;

    private CarSellOrderBoxData carSellOrderBoxData;
    private CarBoxData carBoxData;
    private Optional<RegularBoxData> optionalPaymentBoxData;
    private Optional<RegularBoxData> optionalChangeBoxData;
    private List<Proof<Proposition>> proofs;

    private SidechainBoxesCompanion boxesCompanion;
    private SidechainBoxesDataCompanion boxesDataCompanion;
    private SidechainProofsCompanion proofsCompanion;

    private long fee;
    private long timestamp;

    private List<NoncedBox<Proposition>> newBoxes;
    private List<BoxUnlocker<Proposition>> unlockers;


    @Inject
    CarSellTransaction(@Assisted("inputIds") List<byte[]> inputsIds,
                       @Assisted("carSellOrderData") CarSellOrderBoxData carSellOrderBoxData,
                       @Assisted("carBoxData") CarBoxData carBoxData,
                       @Assisted("optionalPaymentBoxData") Optional<RegularBoxData> optionalPaymentBoxData,
                       @Assisted("optionalChangeBoxData") Optional<RegularBoxData> optionalChangeBoxData,
                       @Assisted("proofs") List<Proof<Proposition>> proofs,
                       @Assisted("fee") long fee,
                       @Assisted("timestamp") long timestamp,
                       SidechainBoxesDataCompanion boxesDataCompanion,
                       SidechainProofsCompanion proofsCompanion) {
        Objects.requireNonNull(inputsIds, "Inputs Ids list can't be null.");
        Objects.requireNonNull(carSellOrderBoxData, "Car Sell Order Data can't be null.");
        Objects.requireNonNull(carBoxData, "Car Box Data can't be null.");
        Objects.requireNonNull(proofs, "Proofs list can't be null.");
        Objects.requireNonNull(boxesDataCompanion, "BoxesDataCompanion can't be null.");
        Objects.requireNonNull(proofsCompanion, "ProofsCompanion can't be null.");
        // Do we need to care about inputs ids length here or state/serialization check is enough?

        this.inputsIds = inputsIds;
        this.carSellOrderBoxData = carSellOrderBoxData;
        this.carBoxData = carBoxData;
        this.optionalPaymentBoxData = optionalPaymentBoxData;
        this.optionalChangeBoxData = optionalChangeBoxData;
        this.proofs = proofs;
        this.fee = fee;
        this.timestamp = timestamp;
        this.boxesDataCompanion = boxesDataCompanion;
        this.proofsCompanion = proofsCompanion;
    }

    @Override
    public TransactionSerializer serializer() {
        return new CarSellTransactionSerializer(boxesDataCompanion, proofsCompanion);
    }

    @Override
    public synchronized List<BoxUnlocker<Proposition>> unlockers() {
        if(unlockers == null) {
            unlockers = new ArrayList<>();
            for (int i = 0; i < inputsIds.size() && i < proofs.size(); i++) {
                int finalI = i;
                BoxUnlocker<Proposition> unlocker = new BoxUnlocker<Proposition>() {
                    @Override
                    public byte[] closedBoxId() {
                        return inputsIds.get(finalI);
                    }

                    @Override
                    public Proof boxKey() {
                        return proofs.get(finalI);
                    }
                };
                unlockers.add(unlocker);
            }
        }

        return Collections.unmodifiableList(unlockers);
    }

    @Override
    public synchronized List<NoncedBox<Proposition>> newBoxes() {
        if(newBoxes == null) {
            newBoxes = new ArrayList<>();

            long nonce = getNewBoxNonce(carBoxData.proposition(), 0);
            newBoxes.add((NoncedBox) carBoxData.getBox(nonce));

            if (optionalPaymentBoxData.isPresent()) {
                nonce = getNewBoxNonce(optionalPaymentBoxData.get().proposition(), 1);
                newBoxes.add((NoncedBox)optionalPaymentBoxData.get().getBox(nonce));
            }

            if (optionalChangeBoxData.isPresent()) {
                nonce = getNewBoxNonce(optionalChangeBoxData.get().proposition(), 2);
                newBoxes.add((NoncedBox)optionalChangeBoxData.get().getBox(nonce));
            }
        }

        return Collections.unmodifiableList(newBoxes);
    }

    @Override
    public long fee() {
        return fee;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public boolean transactionSemanticValidity() {
        if(fee < 0 || timestamp < 0)
            return false;

        if (carSellOrderBoxData.proposition().equals(carBoxData.proposition())) {
            if (inputsIds.size() > 1|| optionalPaymentBoxData.isPresent() ||
                optionalChangeBoxData.isPresent())
                return false;
        } else {
            if (inputsIds.size() != 2)
                return false;

            if (!optionalPaymentBoxData.isPresent())
                return false;

            if (optionalPaymentBoxData.get().value() != carSellOrderBoxData.value() ||
                !optionalPaymentBoxData.get().proposition().equals(carSellOrderBoxData.proposition()))
                return false;
        }

        // check that we have enough proofs and try to open each box only once.
        if(unlockers().size() != proofs.size())
            return false;

        return true;
    }

    @Override
    public byte transactionTypeId() {
        return 10;
    }

    @Override
    public byte[] bytes() {

        ByteArrayOutputStream inputsIdsStream = new ByteArrayOutputStream();
        for(byte[] id: inputsIds)
            inputsIdsStream.write(id, 0, id.length);

        byte[] inputIdsBytes = inputsIdsStream.toByteArray();

        byte[] carSellOrderDataBytes = boxesDataCompanion.toBytes((NoncedBoxData) carSellOrderBoxData);

        byte[] carBoxDataBytes = boxesDataCompanion.toBytes((NoncedBoxData)carBoxData);

        byte[] paymentBoxDataBytes = new byte[0];

        if (optionalPaymentBoxData.isPresent()) {
            paymentBoxDataBytes = boxesDataCompanion.toBytes((NoncedBoxData)optionalPaymentBoxData.get());
        }

        byte[] changeBoxDataBytes = new byte[0];

        if (optionalChangeBoxData.isPresent()) {
            changeBoxDataBytes = boxesDataCompanion.toBytes((NoncedBoxData)optionalChangeBoxData.get());
        }

        ListSerializer<Proof<Proposition>> proofsSerializer = new ListSerializer<>(proofsCompanion, MAX_TRANSACTION_UNLOCKERS);
        byte[] proofsBytes = proofsSerializer.toBytes(proofs);

        return Bytes.concat(                                        // minimum SidechainCoreTransaction length is 36 bytes
                Longs.toByteArray(fee()),                           // 8 bytes
                Longs.toByteArray(timestamp()),                     // 8 bytes
                Ints.toByteArray(inputIdsBytes.length),             // 4 bytes
                inputIdsBytes,                                      // depends in previous value(>=0 bytes)
                Ints.toByteArray(carSellOrderDataBytes.length),     // 4 bytes
                carSellOrderDataBytes,                              // depends on previous value (>=4 bytes)
                Ints.toByteArray(carBoxDataBytes.length),           // 4 bytes
                carBoxDataBytes,                                    // depends on previous value (>=4 bytes)
                Ints.toByteArray(paymentBoxDataBytes.length),
                paymentBoxDataBytes,
                Ints.toByteArray(changeBoxDataBytes.length),
                changeBoxDataBytes,
                Ints.toByteArray(proofsBytes.length),               // 4 bytes
                proofsBytes                                         // depends on previous value (>=4 bytes)
        );
    }

    public static CarSellTransaction parseBytes(
            byte[] bytes,
            SidechainBoxesDataCompanion boxesDataCompanion,
            SidechainProofsCompanion proofsCompanion) {
        if(bytes.length < 36)
            throw new IllegalArgumentException("Input data corrupted.");

        if(bytes.length > MAX_TRANSACTION_SIZE)
            throw new IllegalArgumentException("Input data length is too large.");

        int offset = 0;

        long fee = BytesUtils.getLong(bytes, offset);
        offset += 8;

        long timestamp = BytesUtils.getLong(bytes, offset);
        offset += 8;

        int batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        ArrayList<byte[]> inputsIds = new ArrayList<>();
        while(batchSize > 0) {
            int idLength = NodeViewModifier$.MODULE$.ModifierIdSize();
            inputsIds.add(Arrays.copyOfRange(bytes, offset, offset + idLength));
            offset += idLength;
            batchSize -= idLength;
        }

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        NoncedBoxData carSellOrderData = boxesDataCompanion.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        NoncedBoxData carBoxData = boxesDataCompanion.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
        offset += batchSize;

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        Optional<RegularBoxData> optionalPaymentBoxData = Optional.empty();

        if (batchSize > 0) {
            NoncedBoxData paymentBoxData = boxesDataCompanion.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
            optionalPaymentBoxData = Optional.of((RegularBoxData)paymentBoxData);
            offset += batchSize;
        }

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;

        Optional<RegularBoxData> optionalChangeBoxData = Optional.empty();

        if (batchSize > 0) {
            NoncedBoxData changeBoxData = boxesDataCompanion.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));
            optionalChangeBoxData = Optional.of((RegularBoxData)changeBoxData);
            offset += batchSize;
        }

        batchSize = BytesUtils.getInt(bytes, offset);
        offset += 4;
        if(bytes.length != offset + batchSize)
            throw new IllegalArgumentException("Input data corrupted.");

        ListSerializer<Proof<Proposition>> proofsSerializer = new ListSerializer<>(proofsCompanion, MAX_TRANSACTION_UNLOCKERS);
        List<Proof<Proposition>> proofs = proofsSerializer.parseBytes(Arrays.copyOfRange(bytes, offset, offset + batchSize));

        return new CarSellTransaction(inputsIds, (CarSellOrderBoxData) carSellOrderData, (CarBoxData) carBoxData, optionalPaymentBoxData,
                optionalChangeBoxData, proofs, fee, timestamp, boxesDataCompanion, proofsCompanion);
    }
}*/
