package com.horizen.examples;

import com.horizen.block.SidechainBlock;
import com.horizen.box.Box;
import com.horizen.box.RegularBox;
import com.horizen.examples.car.CarBox;
import com.horizen.examples.car.CarSellOrder;
import com.horizen.proposition.Proposition;
import com.horizen.state.ApplicationState;
import com.horizen.state.SidechainStateReader;
import com.horizen.transaction.BoxTransaction;
import com.horizen.utils.ByteArrayWrapper;
import scala.util.Success;
import scala.util.Try;

import java.util.List;

public class DefaultApplicationState implements ApplicationState {
    @Override
    public boolean validate(SidechainStateReader stateReader, SidechainBlock block) {
        return true;
    }

    @Override
    public boolean validate(SidechainStateReader stateReader, BoxTransaction<Proposition, Box<Proposition>> transaction) {

        for (Box box: transaction.newBoxes()) {
            if (box instanceof CarSellOrder)
                return checkCarSellOrderTransaction(stateReader, transaction);
        }

        return true;
    }

    private boolean checkCarSellOrderTransaction (SidechainStateReader stateReader, BoxTransaction<Proposition, Box<Proposition>> transaction) {
        int carBoxCount = 0;
        int carSellOrderCount = 0;

        for (ByteArrayWrapper id : transaction.boxIdsToOpen()){
            Box box = stateReader.getClosedBox(id.data()).get();
            if (box instanceof CarBox)
                carBoxCount += 1;
        }

        if (carBoxCount != 1)
            return false;

        for (Box box: transaction.newBoxes()) {
            if (box instanceof CarSellOrder)
                carSellOrderCount += 1;
        }

        if (carSellOrderCount != 1)
            return false;

        return true;
    }

    @Override
    public Try<ApplicationState> onApplyChanges(SidechainStateReader stateReader, byte[] version, List<Box<Proposition>> newBoxes, List<byte[]> boxIdsToRemove) {
        return new Success<>(this);
    }

    @Override
    public Try<ApplicationState> onRollback(byte[] version) {
        return new Success<>(this);
    }
}
