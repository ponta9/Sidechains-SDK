package com.horizen.examples.car;

import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.horizen.api.http.ApiResponse;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.api.http.ErrorResponse;
import com.horizen.api.http.SuccessResponse;
import com.horizen.box.NoncedBox;
import com.horizen.box.RegularBox;
import com.horizen.box.data.NoncedBoxData;
import com.horizen.box.data.RegularBoxData;
import com.horizen.companion.SidechainTransactionsCompanion;
import com.horizen.node.SidechainNodeView;
import com.horizen.proof.Proof;
import com.horizen.proposition.Proposition;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.serialization.Views;
import com.horizen.transaction.BoxTransaction;
import com.horizen.transaction.SidechainCoreTransaction;
import com.horizen.transaction.SidechainCoreTransactionFactory;
import com.horizen.transaction.SidechainTransaction;
import com.horizen.utils.BytesUtils;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import scala.Option;
import scala.Some;

import com.horizen.box.Box;

import java.math.BigInteger;
import java.util.*;
import java.util.Optional;

//simple way to add description for usage in swagger?
public class CarApi extends ApplicationApiGroup
{

  private final SidechainTransactionsCompanion sidechainTransactionsCompanion;

  public CarApi(SidechainTransactionsCompanion sidechainTransactionsCompanion) {
    this.sidechainTransactionsCompanion = sidechainTransactionsCompanion;
  }
  @Override
  public String basePath() {
    return "carApi";
  }

  @Override
  public List<Route> getRoutes() {
    List<Route> routes = new ArrayList<>();
    routes.add(bindPostRequest("createCar", this::createCar, CreateCarBoxRequest.class));
    routes.add(bindPostRequest("createCarSellOrder", this::createCarSellOrder, CreateCarSellOrderRequest.class));
    routes.add(bindPostRequest("acceptCarSellOrder", this::acceptCarSellOrder, AcceptCarSellOrderRequest.class));
    return routes;
  }

  private ApiResponse createCar(SidechainNodeView view, CreateCarBoxRequest ent) {
    CarBoxData carBoxData = new CarBoxData(ent.carProposition, 1, ent.vin, ent.year, ent.model, ent.color, ent.description);

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

    List<NoncedBoxData<Proposition, NoncedBox<Proposition>>> outputs = new ArrayList();

    outputs.add(output);
    outputs.add((NoncedBoxData) carBoxData);

    SidechainCoreTransaction unsignedTransaction =
        getSidechainCoreTransactionFactory().create(inputIds, outputs, fakeProofs, ent.fee, timestamp);
    byte[] messageToSign = unsignedTransaction.messageToSign();

    Proof proof = view.getNodeWallet().secretByPublicKey(inputBox.proposition()).get().sign(messageToSign);

    SidechainCoreTransaction signedTransaction =
        getSidechainCoreTransactionFactory().create(inputIds, outputs, Collections.singletonList(proof), ent.fee, timestamp);

    CarResponse result = new CarResponse(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction)signedTransaction)));
    return result;
  }

  private ApiResponse createCarSellOrder(SidechainNodeView view, CreateCarSellOrderRequest ent) {
    try {
      long timestamp = System.currentTimeMillis();
      long fee = 0;
      CarBox carBox = null;

      for (Box b : view.getNodeWallet().boxesOfType(CarBox.class)) {
        if (Arrays.equals(b.id(), BytesUtils.fromHexString(ent.carBoxId)))
          carBox = (CarBox) b;
      }

      if (carBox == null)
        throw new IllegalArgumentException("CarBox not found.");

      List<byte[]> inputIds = new ArrayList<byte[]>();
      inputIds.add(carBox.id());

      List<NoncedBoxData<Proposition, NoncedBox<Proposition>>> outputs = new ArrayList();
      CarSellOrderData carSellOrderData = new CarSellOrderData(ent.proposition, ent.sellPrice,
              carBox.getBoxData().getVin(), carBox.proposition(),
              carBox.getBoxData().getYear(), carBox.getBoxData().getModel(),
              carBox.getBoxData().getColor(), carBox.getBoxData().getDescription());
      outputs.add((NoncedBoxData)carSellOrderData);

      List<Proof<Proposition>> fakeProofs = Collections.nCopies(inputIds.size(), null);

      SidechainCoreTransaction unsignedTransaction = getSidechainCoreTransactionFactory().create(inputIds, outputs, fakeProofs, fee, timestamp);

      byte[] messageToSign = unsignedTransaction.messageToSign();

      List<Proof<Proposition>> proofs = new ArrayList<>();

      proofs.add(view.getNodeWallet().secretByPublicKey(carBox.proposition()).get().sign(messageToSign));

      SidechainCoreTransaction transaction = getSidechainCoreTransactionFactory().create(inputIds, outputs, proofs, fee, timestamp);

      return new CreateCarSellOrderResponce(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction)transaction)));
    } catch (Exception e) {
      return new CarResponseError("0102", "Error during Car Sell Order creation.", Some.apply(e));
    }
  }

  private ApiResponse acceptCarSellOrder(SidechainNodeView view, AcceptCarSellOrderRequest ent) {
    try {
      long timestamp = System.currentTimeMillis();
      long fee = 0;
      CarSellOrder carSellOrder = null;
      RegularBox paymentBox = null;

      for (Box b : view.getNodeWallet().boxesOfType(CarSellOrder.class)) {
        if (Arrays.equals(b.id(), BytesUtils.fromHexString(ent.carSellOrderId)))
          carSellOrder = (CarSellOrder) b;
      }

      if (carSellOrder == null)
        throw new IllegalArgumentException("CarSellOrder not found.");

      for (Box b : view.getNodeWallet().boxesOfType(RegularBox.class))
        if (Arrays.equals(b.id(), BytesUtils.fromHexString(ent.paymentRegularBoxId)))
          paymentBox = (RegularBox) b;

      if (paymentBox == null)
        throw new IllegalArgumentException("RegularBox to spend is not found.");

      List<byte[]> inputIds = new ArrayList<byte[]>();
      inputIds.add(carSellOrder.id());
      inputIds.add(paymentBox.id());

      List<NoncedBoxData<Proposition, NoncedBox<Proposition>>> outputs = new ArrayList();
      CarBoxData carBoxData = new CarBoxData(ent.buyerProposition, 1,
              carSellOrder.getBoxData().getVin(), carSellOrder.getBoxData().getYear(),
              carSellOrder.getBoxData().getModel(), carSellOrder.getBoxData().getColor(),
              carSellOrder.getBoxData().getDescription());
      outputs.add((NoncedBoxData)carBoxData);

      if (paymentBox.value() < carSellOrder.value())
        throw new IllegalArgumentException("RegularBox to spend does not contain enough coins.");

      RegularBoxData paymentData = new RegularBoxData(carSellOrder.getBoxData().getSellerProposition(), carSellOrder.value());
      outputs.add((NoncedBoxData)paymentData);


      if (paymentBox.value() > carSellOrder.value()) {
        RegularBoxData differenceData = new RegularBoxData(ent.buyerProposition, paymentBox.value() - carSellOrder.value());
        outputs.add((NoncedBoxData)differenceData);
      }

      List<Proof<Proposition>> fakeProofs = Collections.nCopies(inputIds.size(), null);

      SidechainCoreTransaction unsignedTransaction = getSidechainCoreTransactionFactory().create(inputIds, outputs, fakeProofs, fee, timestamp);

      byte[] messageToSign = unsignedTransaction.messageToSign();

      List<Proof<Proposition>> proofs = new ArrayList<>();

      proofs.add(view.getNodeWallet().secretByPublicKey(carSellOrder.proposition()).get().sign(messageToSign));

      if (paymentBox.value() > carSellOrder.value())
        proofs.add(view.getNodeWallet().secretByPublicKey(ent.buyerProposition).get().sign(messageToSign));

      SidechainCoreTransaction transaction = getSidechainCoreTransactionFactory().create(inputIds, outputs, proofs, fee, timestamp);

      return new AcceptCarSellOrderResponce(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction)transaction)));
    } catch (Exception e) {
      return new CarResponseError("0103", "Error.", Some.apply(e));
    }
  }

  public static class CreateCarBoxRequest {
    BigInteger vin;
    PublicKey25519Proposition carProposition;
    int year;
    String model;
    String color;
    String description;

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

    public int getYear() {
      return year;
    }

    public void setYear(int year) {
      this.year = year;
    }

    public String getModel() {
      return model;
    }

    public void setModel(String model) {
      this.model = model;
    }

    public String getColor() {
      return color;
    }

    public void setColor(String color) {
      this.color = color;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
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

  public static class CreateCarSellOrderRequest {
    String carBoxId;
    PublicKey25519Proposition proposition;
    long sellPrice;

    public String getCarBoxId()
    {
      return carBoxId;
    }

    public void setCarBoxId(String carBoxId)
    {
      this.carBoxId = carBoxId;
    }

    public PublicKey25519Proposition getProposition()
    {
      return proposition;
    }

    public void setProposition(String propositionHexBytes)
    {
      byte[] propositionBytes = BytesUtils.fromHexString(propositionHexBytes);
      proposition = new PublicKey25519Proposition(propositionBytes);
    }

    public long getSellPrice() { return sellPrice; }

    public void setSellPrice(long sellPrice) { this.sellPrice = sellPrice; }
  }

  @JsonView(Views.Default.class)
  class CreateCarSellOrderResponce implements SuccessResponse{
    private final String carSellOrderTxBytes;

    public CreateCarSellOrderResponce(String carSellOrderTxBytes) {
      this.carSellOrderTxBytes = carSellOrderTxBytes;
    }

    public String carSellOrderTxBytes() { return carSellOrderTxBytes;}

    public String getCarSellOrderTxBytes() {
      return carSellOrderTxBytes;
    }
  }

  public static class AcceptCarSellOrderRequest {
    String carSellOrderId;
    String paymentRegularBoxId;
    PublicKey25519Proposition buyerProposition;

    public String getCarSellOrderId()
    {
      return carSellOrderId;
    }

    public void setCarSellOrderId(String carSellOrderId)
    {
      this.carSellOrderId = carSellOrderId;
    }

    public String getPaymentRegularBoxId()
    {
      return paymentRegularBoxId;
    }

    public void setPaymentRegularBoxId(String paymentRegularBoxId)
    {
      this.paymentRegularBoxId = paymentRegularBoxId;
    }

    public PublicKey25519Proposition getBuyerProposition()
    {
      return buyerProposition;
    }

    public void setBuyerProposition(String propositionHexBytes)
    {
      byte[] propositionBytes = BytesUtils.fromHexString(propositionHexBytes);
      buyerProposition = new PublicKey25519Proposition(propositionBytes);
    }

  }

  @JsonView(Views.Default.class)
  class AcceptCarSellOrderResponce implements SuccessResponse{
    private final String acceptedCarSellOrderTxBytes;

    public AcceptCarSellOrderResponce(String acceptedCarSellOrderTxBytes) {
      this.acceptedCarSellOrderTxBytes = acceptedCarSellOrderTxBytes;
    }

    public String acceptedCarSellOrderTxBytes() { return acceptedCarSellOrderTxBytes;}

    public String getAcceptedCarSellOrderTxBytes() {
      return acceptedCarSellOrderTxBytes;
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

