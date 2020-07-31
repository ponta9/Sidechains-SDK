package com.horizen.examples.car;


import com.horizen.companion.SidechainBoxesCompanion;
import com.horizen.companion.SidechainBoxesDataCompanion;
import com.horizen.companion.SidechainProofsCompanion;
import com.horizen.transaction.TransactionSerializer;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

/*public final class CarSellTransactionSerializer implements TransactionSerializer<CarSellTransaction>
{
    private SidechainBoxesDataCompanion boxesDataCompanion;
    private SidechainProofsCompanion proofsCompanion;

    public CarSellTransactionSerializer(SidechainBoxesDataCompanion boxesDataCompanion,
                                        SidechainProofsCompanion proofsCompanion) {
        this.boxesDataCompanion = boxesDataCompanion;
        this.proofsCompanion = proofsCompanion;
    }

    @Override
    public void serialize(CarSellTransaction transaction, Writer writer) {
        writer.putBytes(transaction.bytes());
    }

    @Override
    public CarSellTransaction parse(Reader reader) {
        return CarSellTransaction.parseBytes(reader.getBytes(reader.remaining()), boxesDataCompanion, proofsCompanion);
    }
}*/
