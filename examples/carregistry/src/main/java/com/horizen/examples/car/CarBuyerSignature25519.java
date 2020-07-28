package com.horizen.examples.car;

import com.google.common.primitives.Bytes;
import com.horizen.proof.AbstractSignature25519;
import com.horizen.proof.ProofSerializer;
import com.horizen.proof.Signature25519Serializer;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.secret.PrivateKey25519;
import com.horizen.utils.Ed25519;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;

import java.util.Arrays;

public final class CarBuyerSignature25519 extends AbstractSignature25519<PrivateKey25519, PublicKey25519Proposition>
{
    public static int SIGNATURE_LENGTH = Ed25519.signatureLength();

    private final PublicKey25519Proposition buyerProposition;

    public CarBuyerSignature25519(byte[] signatureBytes, PublicKey25519Proposition buyerProposition) {
        super(signatureBytes);
        if (signatureBytes.length != SIGNATURE_LENGTH)
            throw new IllegalArgumentException(String.format("Incorrect signature length, %d expected, %d found", SIGNATURE_LENGTH,
                    signatureBytes.length));
        this.buyerProposition = buyerProposition;
    }

    @Override
    public boolean isValid(PublicKey25519Proposition proposition, byte[] message) {
        return Ed25519.verify(signatureBytes, message, buyerProposition.pubKeyBytes());
    }

    @Override
    public byte proofTypeId() {
        return 10;
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(signatureBytes, buyerProposition.bytes());
    }

    @Override
    public ProofSerializer serializer() {
        return CarBuyerSignature25519Serializer.getSerializer();
    }

    public static CarBuyerSignature25519 parseBytes(byte[] bytes) {
        byte[] signatureBytes = Arrays.copyOf(bytes, SIGNATURE_LENGTH);
        byte[] buyerPropositionBytes = Arrays.copyOfRange(bytes, SIGNATURE_LENGTH, SIGNATURE_LENGTH + PublicKey25519Proposition.KEY_LENGTH);

        return new CarBuyerSignature25519(signatureBytes, new PublicKey25519Proposition(buyerPropositionBytes));
    }

    @Override
    public String toString() {
        return "CarBuyerSignature25519{" +
                "signatureBytes=" + ByteUtils.toHexString(signatureBytes) +
                "buyerProposition=" + ByteUtils.toHexString(buyerProposition.pubKeyBytes()) +
                '}';
    }
}
