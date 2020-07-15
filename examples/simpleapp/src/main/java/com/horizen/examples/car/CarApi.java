package com.horizen.examples.car;

import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.horizen.api.http.ApiResponse;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.api.http.ErrorResponse;
import com.horizen.api.http.SuccessResponse;
import com.horizen.box.Box;
import com.horizen.box.NoncedBox;
import com.horizen.box.RegularBox;
import com.horizen.box.data.NoncedBoxData;
import com.horizen.box.data.RegularBoxData;
import com.horizen.node.NodeWallet;
import com.horizen.node.SidechainNodeView;
import com.horizen.proof.Proof;
import com.horizen.proposition.Proposition;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.serialization.Views;
import com.horizen.transaction.SidechainCoreTransaction;
import com.horizen.utils.BytesUtils;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import scala.Array;
import scala.None;
import scala.Option;
import scala.sys.Prop;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

//simple way to add description for usage in swagger?
public class CarApi extends ApplicationApiGroup
{
  @Override
  public String basePath() {
    return "carApi";
  }

  @Override
  public List<Route> getRoutes() {
    List<Route> routes = new ArrayList<>();
    routes.add(bindPostRequest("createCar", this::createCar, CreateCarBoxRequest.class));
    return routes;
  }

  private ApiResponse createCar(SidechainNodeView view, CreateCarBoxRequest ent) {
    CarBoxData carBoxData = new CarBoxData(ent.carProposition, 1, ent.vin);

    Optional<Box<Proposition>> inputBoxOpt  = view.getNodeWallet().allBoxes().stream().filter(box -> BytesUtils.toHexString(box.id()).equals(ent.boxId)).findFirst();
    if (!inputBoxOpt.isPresent()) {
      return new CarResponseError("0100", "Box for paying fee is not found", Option.empty()); //change API response to use java optional
    }

    Box<Proposition> inputBox = inputBoxOpt.get();

    long change = inputBox.value() - ent.fee;
    if (change < 0) {
      return new CarResponseError("0101", "Box for paying fee doesn't have enough coins to paid fee", Option.empty()); //change API response to use java optional
    }

    NoncedBoxData output = new RegularBoxData((PublicKey25519Proposition) inputBox.proposition(), change);


    List<byte[]> inputIds = Collections.singletonList(BytesUtils.fromHexString(ent.boxId));
    Long timestamp = System.currentTimeMillis();
    List fakeProofs = Collections.nCopies(inputIds.size(), null);
    List outputs = Collections.singletonList(output);

    SidechainCoreTransaction unsignedTransaction =
        getSidechainCoreTransactionFactory().create(inputIds, outputs, fakeProofs, ent.fee, timestamp);
    byte[] messageToSign = unsignedTransaction.messageToSign();
    Proof proof = view.getNodeWallet().secretByPublicKey(inputBox.proposition()).get().sign(messageToSign);

    SidechainCoreTransaction signedTransaction =
        getSidechainCoreTransactionFactory().create(inputIds, outputs, Collections.singletonList(proof), ent.fee, timestamp);
    CarResponse result = new CarResponse(ByteUtils.toHexString(signedTransaction.bytes()));
    return result;
  }

  public static class CreateCarBoxRequest {
    BigInteger vin;
    PublicKey25519Proposition carProposition;

    int fee;
    String boxId;


    public BigInteger getVin()
    {
      return vin;
    }

    public void setVin(String vin)
    {
      this.vin = new BigInteger(vin);
    }

    public PublicKey25519Proposition getCarProposition()
    {
      return carProposition;
    }

    public void setCarProposition(String propositionHexBytes)
    {
      byte[] propositionBytes = BytesUtils.fromHexString(propositionHexBytes);
      carProposition = new PublicKey25519Proposition(propositionBytes);
    }

    public int getFee()
    {
      return fee;
    }

    public void setFee(int fee)
    {
      this.fee = fee;
    }

    public String getBoxId()
    {
      return boxId;
    }

    public void setBoxId(String boxIdAsString)
    {
      boxId = boxIdAsString;
    }
  }

  @JsonView(Views.Default.class)
  class CarResponse implements SuccessResponse{
    private final String createCarTxBytes;

    public CarResponse(String createCarTxBytes)
    {
      this.createCarTxBytes = createCarTxBytes;
    }

    public String carTxBytes()
    {
      return createCarTxBytes;
    }

    public String getCreateCarTxBytes()
    {
      return createCarTxBytes;
    }
  }

  static class CarResponseError implements ErrorResponse {
    private final String code;
    private final String description;
    private final Option<Throwable> exception;

    CarResponseError(String code, String description, Option<Throwable> exception) {
      this.code = code;
      this.description = description;
      this.exception = exception;
    }

    @Override
    public String code()
    {
      return null;
    }

    @Override
    public String description()
    {
      return null;
    }

    @Override
    public Option<Throwable> exception()
    {
      return null;
    }
  }
}

