package com.horizen.examples;

import com.horizen.box.Box;
import com.horizen.proposition.Proposition;
import com.horizen.secret.Secret;
import com.horizen.wallet.ApplicationWallet;

import java.util.List;

// There is no custom logic for Car registry Wallet now.
// TODO: detect the CarSellOrderBoxes where buyer proposition is related to the Wallet secrets.
public class CarRegistryApplicationWallet implements ApplicationWallet {

    @Override
    public void onAddSecret(Secret secret) {

    }

    @Override
    public void onRemoveSecret(Proposition proposition) {

    }

    @Override
    public void onChangeBoxes(byte[] version, List<Box<Proposition>> boxesToUpdate, List<byte[]> boxIdsToRemove) {

    }

    @Override
    public void onRollback(byte[] version) {

    }
}
