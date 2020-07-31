package com.horizen.examples.car.transaction;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.horizen.box.*;
import com.horizen.box.data.*;
import com.horizen.proof.Proof;
import com.horizen.proof.Signature25519;
import com.horizen.proof.Signature25519Serializer;
import com.horizen.proposition.Proposition;
import com.horizen.transaction.SidechainTransaction;
import com.horizen.utils.ListSerializer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRegularTransaction extends SidechainTransaction<Proposition, NoncedBox<Proposition>> {

    protected List<byte[]> inputRegularBoxIds;
    protected List<Signature25519> inputRegularBoxProofs;
    protected List<RegularBoxData> outputRegularBoxesData;

    protected long fee;
    protected long timestamp;

    protected static ListSerializer<Signature25519> regularBoxProofsSerializer =
            new ListSerializer<>(Signature25519Serializer.getSerializer(), MAX_TRANSACTION_UNLOCKERS);
    protected static ListSerializer<RegularBoxData> regularBoxDataListSerializer =
            new ListSerializer<>(RegularBoxDataSerializer.getSerializer(), MAX_TRANSACTION_NEW_BOXES);

    public AbstractRegularTransaction(List<byte[]> inputRegularBoxIds,

                                      List<Signature25519> inputRegularBoxProofs,
                                      List<RegularBoxData> outputRegularBoxesData,
                                      long fee,
                                      long timestamp) {
        if(inputRegularBoxIds.size() != inputRegularBoxProofs.size())
            throw new IllegalArgumentException("Regular box inputs list size is different to proving signatures list size!");

        this.inputRegularBoxIds = inputRegularBoxIds;
        this.inputRegularBoxProofs = inputRegularBoxProofs;
        this.outputRegularBoxesData = outputRegularBoxesData;
        this.fee = fee;
        this.timestamp = timestamp;
    }


    @Override
    public List<BoxUnlocker<Proposition>> unlockers() {
        List<BoxUnlocker<Proposition>> unlockers = new ArrayList<>();
        for (int i = 0; i < inputRegularBoxIds.size() && i < inputRegularBoxProofs.size(); i++) {
            int finalI = i;
            BoxUnlocker<Proposition> unlocker = new BoxUnlocker<Proposition>() {
                @Override
                public byte[] closedBoxId() {
                    return inputRegularBoxIds.get(finalI);
                }

                @Override
                public Proof boxKey() {
                    return inputRegularBoxProofs.get(finalI);
                }
            };
            unlockers.add(unlocker);
        }

        return unlockers;
    }

    @Override
    public List<NoncedBox<Proposition>> newBoxes() {
        List<NoncedBox<Proposition>> newBoxes = new ArrayList<>();
        for (int i = 0; i < outputRegularBoxesData.size(); i++) {
            long nonce = getNewBoxNonce(outputRegularBoxesData.get(i).proposition(), i);
            RegularBoxData boxData = outputRegularBoxesData.get(i);
            newBoxes.add((NoncedBox)new RegularBox(boxData, nonce));
        }
        return newBoxes;
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

        // check that we have enough proofs.
        if(inputRegularBoxIds.size() != inputRegularBoxProofs.size())
            return false;

        return true;
    }
}
