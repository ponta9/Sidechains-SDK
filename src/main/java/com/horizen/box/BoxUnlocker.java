package com.horizen.box;
//import scorex.core.utils.ScorexEncoder;

import com.horizen.proof.Proof;
import com.horizen.proposition.Proposition;

public interface BoxUnlocker<P extends Proposition> extends scorex.core.transaction.box.BoxUnlocker
{
    @Override
    byte[] closedBoxId();

    @Override
    Proof<P> boxKey();

    @Override
    String toString();

    // To Do: check this
    //@Override
    //ScorexEncoder encoder();
}
