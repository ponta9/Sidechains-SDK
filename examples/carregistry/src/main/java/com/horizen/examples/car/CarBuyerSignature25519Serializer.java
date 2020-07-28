package com.horizen.examples.car;

import com.horizen.proof.ProofSerializer;
import com.horizen.proof.Signature25519;
import scorex.util.serialization.Reader;
import scorex.util.serialization.Writer;

public final class CarBuyerSignature25519Serializer implements ProofSerializer<CarBuyerSignature25519> {

    private static CarBuyerSignature25519Serializer serializer;

    static {
        serializer = new CarBuyerSignature25519Serializer();
    }

    private CarBuyerSignature25519Serializer() {
        super();
    }

    public static CarBuyerSignature25519Serializer getSerializer() {
        return serializer;
    }

    @Override
    public void serialize(CarBuyerSignature25519 signature, Writer writer) {
        writer.putBytes(signature.bytes());
    }

    @Override
    public CarBuyerSignature25519 parse(Reader reader) {
        return CarBuyerSignature25519.parseBytes(reader.getBytes(reader.remaining()));
    }

}
