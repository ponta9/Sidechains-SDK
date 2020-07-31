package com.horizen.examples.car.api;

import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonView;
import com.horizen.api.http.ApiResponse;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.api.http.ErrorResponse;
import com.horizen.api.http.SuccessResponse;
import com.horizen.box.NoncedBox;
import com.horizen.box.RegularBox;
import com.horizen.box.data.NoncedBoxData;
import com.horizen.box.data.RegularBoxData;
import com.horizen.companion.SidechainBoxesDataCompanion;
import com.horizen.companion.SidechainProofsCompanion;
import com.horizen.companion.SidechainTransactionsCompanion;
import com.horizen.examples.car.api.request.CreateCarBoxRequest;
import com.horizen.examples.car.box.CarBox;
import com.horizen.examples.car.box.CarSellOrderBox;
import com.horizen.examples.car.box.data.CarBoxData;
import com.horizen.examples.car.box.data.CarSellOrderBoxData;
import com.horizen.examples.car.proposition.SellOrderProposition;
import com.horizen.examples.car.transaction.CarDeclarationTransaction;
import com.horizen.node.NodeMemoryPool;
import com.horizen.node.SidechainNodeView;
import com.horizen.proof.Proof;
import com.horizen.proof.Signature25519;
import com.horizen.proposition.Proposition;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.proposition.PublicKey25519PropositionSerializer;
import com.horizen.serialization.Views;
import com.horizen.transaction.BoxTransaction;
import com.horizen.transaction.SidechainCoreTransaction;
import com.horizen.utils.ByteArrayWrapper;
import com.horizen.utils.BytesUtils;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import scala.Option;
import scala.Some;

import com.horizen.box.Box;
import scala.sys.Prop;

import java.math.BigInteger;
import java.util.*;
import java.util.Optional;

//simple way to add description for usage in swagger?
public class CarApi extends ApplicationApiGroup {

    private final SidechainTransactionsCompanion sidechainTransactionsCompanion;
    private final SidechainBoxesDataCompanion sidechainBoxesDataCompanion;
    private final SidechainProofsCompanion sidechainProofsCompanion;

    public CarApi(SidechainTransactionsCompanion sidechainTransactionsCompanion,
                  SidechainBoxesDataCompanion sidechainBoxesDataCompanion,
                  SidechainProofsCompanion sidechainProofsCompanion) {
        this.sidechainTransactionsCompanion = sidechainTransactionsCompanion;
        this.sidechainBoxesDataCompanion = sidechainBoxesDataCompanion;
        this.sidechainProofsCompanion = sidechainProofsCompanion;
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
        routes.add(bindPostRequest("cancelCarSellOrder", this::cancelCarSellOrder, CancelCarSellOrderRequest.class));
        return routes;
    }

    /*
      Route to create car (register new car in the Sidechain).
      Input parameters are car properties and fee amount to pay.
      Route checks if the is enough regular box balance to pay fee and then creates CarDeclarationTransaction.
      Output of this transaction is new Car Box token.
      Returns the hex representation of the transaction.
    */
    private ApiResponse createCar(SidechainNodeView view, CreateCarBoxRequest ent) {

        PublicKey25519Proposition carOwnershipProposition = PublicKey25519PropositionSerializer.getSerializer().parseBytes(BytesUtils.fromHexString(ent.proposition));

        CarBoxData carBoxData = new CarBoxData(carOwnershipProposition, ent.vin, ent.year, ent.model, ent.color);

        List<byte[]> boxIdsToExclude = boxesFromMempool(view.getNodeMemoryPool());

        List<Box<Proposition>> paymentBoxes = new ArrayList<>();

        long amountToPay = ent.fee;

        List<Box<Proposition>> regularBoxes = view.getNodeWallet().boxesOfType(RegularBox.class, boxIdsToExclude);
        int index = 0;
        while(amountToPay > 0 && index < regularBoxes.size()) {
            paymentBoxes.add(regularBoxes.get(index));
            amountToPay -= regularBoxes.get(index).value();
            index++;
        }

        if(amountToPay > 0) {
            return new CarResponseError("0100", "Not enough coins to buy the car.", Option.empty()); //change API response to use java optional
        }

        long change = Math.abs(amountToPay);
        List<RegularBoxData> regularOutputs = new ArrayList<>();
        if(change > 0)
            regularOutputs.add(new RegularBoxData((PublicKey25519Proposition) paymentBoxes.get(0).proposition(), change));

        List<byte[]> inputIds = new ArrayList<>();
        for(Box b : paymentBoxes)
            inputIds.add(b.id());

        List fakeProofs = Collections.nCopies(inputIds.size(), null);
        Long timestamp = System.currentTimeMillis();

        CarDeclarationTransaction unsignedTransaction = new CarDeclarationTransaction(
                inputIds,
                fakeProofs,
                regularOutputs,
                carBoxData,
                ent.fee,
                timestamp);

        byte[] messageToSign = unsignedTransaction.messageToSign();
        List<Signature25519> proofs = new ArrayList<>();
        for(Box<Proposition> box : paymentBoxes) {
            proofs.add((Signature25519)view.getNodeWallet().secretByPublicKey(box.proposition()).get().sign(messageToSign));
        }

        CarDeclarationTransaction signedTransaction = new CarDeclarationTransaction(
                inputIds,
                proofs,
                regularOutputs,
                carBoxData,
                ent.fee,
                timestamp);

        return new CarResponse(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction) signedTransaction)));
    }

    /*
      Route to create car sell order.
      Input parameters are Car Box and sell price.
      Route checks if car box exists and then creates Sidechain Core transaction.
      Output of this transaction is new Car Sell Order.
      Hex representation of new transaction is returned.
    */
    private ApiResponse createCarSellOrder(SidechainNodeView view, CreateCarSellOrderRequest ent) {
        return new CarResponse("");
        /*
        try {
            long timestamp = System.currentTimeMillis();
            CarBox carBox = null;

            for (Box b : view.getNodeWallet().boxesOfType(CarBox.class)) {
                if (Arrays.equals(b.id(), BytesUtils.fromHexString(ent.carBoxId)))
                    carBox = (CarBox) b;
            }

            if (carBox == null)
                throw new IllegalArgumentException("CarBox not found.");

            List<byte[]> inputIds = new ArrayList<>();
            inputIds.add(carBox.id());

            List<NoncedBoxData<Proposition, NoncedBox<Proposition>>> outputs = new ArrayList<>();
            CarSellOrderBoxData carSellOrderBoxData = new CarSellOrderBoxData(
                    new SellOrderProposition(carBox.proposition().pubKeyBytes()),
                    ent.sellPrice,
                    carBox.getVin(),
                    carBox.getYear(),
                    carBox.getModel(),
                    carBox.getColor(),
                    carBox.getDescription());
            outputs.add((NoncedBoxData) carSellOrderBoxData);

            List<Proof<Proposition>> fakeProofs = Collections.nCopies(inputIds.size(), null);

            SidechainCoreTransaction unsignedTransaction = getSidechainCoreTransactionFactory().create(inputIds, outputs, fakeProofs, ent.fee, timestamp);

            byte[] messageToSign = unsignedTransaction.messageToSign();

            List<Proof<Proposition>> proofs = new ArrayList<>();

            proofs.add(view.getNodeWallet().secretByPublicKey(carBox.proposition()).get().sign(messageToSign));

            SidechainCoreTransaction transaction = getSidechainCoreTransactionFactory().create(inputIds, outputs, proofs, ent.fee, timestamp);

            return new CreateCarSellOrderResponce(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction) transaction)));
        } catch (Exception e) {
            return new CarResponseError("0102", "Error during Car Sell Order creation.", Some.apply(e));
        }*/
    }

    /*
      Route to accept car sell order.
      Input parameters are Car Sell Order, regular box to pay for car and proposition of the buyer.
      Route checks if car sell order and regular box to pay exist and then creates Sidechain Core transaction.
      Output of this transaction is new Car Box (with buyer as owner) and regular box with coins amount
      equivalent to sell price as payment for car to previous car owner.
      Hex representation of new transaction is returned.
    */
    private ApiResponse acceptCarSellOrder(SidechainNodeView view, AcceptCarSellOrderRequest ent) {
        return new CarResponse("");
        /*
        try {
            long timestamp = System.currentTimeMillis();
            long fee = 0;
            // TODO: make it safer, check on each step and return proper error.
            CarSellOrderBox carSellOrder = (CarSellOrderBox)view.getNodeState().getClosedBox(BytesUtils.fromHexString(ent.carSellOrderId)).get();
            List<Box<Proposition>> paymentBoxes = new ArrayList<>();

            //if (carSellOrder == null)
            //    throw new IllegalArgumentException("CarSellOrder not found.");

            long amountToPay = carSellOrder.value();
            List<Box<Proposition>> regularBoxes = view.getNodeWallet().boxesOfType(RegularBox.class);
            int index = 0;
            while(amountToPay > 0 && index < regularBoxes.size()) {
                paymentBoxes.add(regularBoxes.get(index));
                amountToPay -= regularBoxes.get(index).value();
                index++;
            }
            if(amountToPay > 0) {
                return new CarResponseError("0100", "Not enough coins to buy the car.", Option.empty()); //change API response to use java optional
            }

            long change = Math.abs(amountToPay);
            if (change < 0) {
                return new CarResponseError("0101", "Box for paying fee doesn't have enough coins to pay the fee", Option.empty()); //change API response to use java optional
            }

            NoncedBoxData changeOutput = new RegularBoxData((PublicKey25519Proposition) paymentBoxes.get(0).proposition(), change);

            List<byte[]> inputIds = new ArrayList<>();
            inputIds.add(carSellOrder.id());
            for(Box b : paymentBoxes)
                inputIds.add(b.id());

            CarBoxData carBoxData = new CarBoxData(ent.buyerProposition,
                    carSellOrder.getVin(),
                    carSellOrder.getYear(),
                    carSellOrder.getModel(),
                    carSellOrder.getColor(),
                    carSellOrder.getDescription());

            if (inputBox.value() < carSellOrder.value())
                throw new IllegalArgumentException("RegularBox to spend does not contain enough coins.");

            RegularBoxData paymentBoxData = new RegularBoxData(carSellOrder.getBoxData().getSellerProposition(), carSellOrder.value());

            Optional<RegularBoxData> optionalChangeData = Optional.empty();

            if (inputBox.value() > carSellOrder.value()) {
                optionalChangeData = Optional.of(new RegularBoxData(ent.buyerProposition, inputBox.value() - carSellOrder.value()));
            }

            List<Proof<Proposition>> fakeProofs = Collections.nCopies(2, null);

            CarSellTransaction unsignedTransaction = new CarSellTransaction(inputIds, carSellOrder.getBoxData(), carBoxData,
                    Optional.of(paymentBoxData), optionalChangeData, fakeProofs, fee, timestamp,
                    sidechainBoxesDataCompanion, sidechainProofsCompanion);

            byte[] messageToSign = unsignedTransaction.messageToSign();

            List<Proof<Proposition>> proofs = new ArrayList<>();

            proofs.add((Proof) new CarBuyerSignature25519(
                    view.getNodeWallet().secretByPublicKey(ent.buyerProposition).get().sign(messageToSign).bytes(),
                    ent.buyerProposition));

            proofs.add(view.getNodeWallet().secretByPublicKey(ent.buyerProposition).get().sign(messageToSign));

            CarSellTransaction transaction = new CarSellTransaction(inputIds, carSellOrder.getBoxData(), carBoxData,
                    Optional.of(paymentBoxData), optionalChangeData, proofs, fee, timestamp,
                    sidechainBoxesDataCompanion, sidechainProofsCompanion);

            return new AcceptCarSellOrderResponce(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction) transaction)));
        } catch (Exception e) {
            return new CarResponseError("0103", "Error.", Some.apply(e));
        }*/
    }

    /*
      Route to cancel car sell order. Car Sell order can be cancelled by owner only.
      Input parameters are Car Sell Order.
      Route checks if car sell order exist and then creates Sidechain Core transaction.
      Output of this transaction is new Car Box (with seller as owner).
      Transaction does not spend any coin boxes.
      Hex representation of new transaction is returned.
    */
    private ApiResponse cancelCarSellOrder(SidechainNodeView view, CancelCarSellOrderRequest ent) {
        return new CarResponse("");
        /*
        try {
            long timestamp = System.currentTimeMillis();
            long fee = 0;
            CarSellOrderBox carSellOrder = null;

            for (Box b : view.getNodeWallet().boxesOfType(CarSellOrderBox.class)) {
                if (Arrays.equals(b.id(), BytesUtils.fromHexString(ent.carSellOrderId)))
                    carSellOrder = (CarSellOrderBox) b;
            }

            if (carSellOrder == null)
                throw new IllegalArgumentException("CarSellOrder not found.");

            List<byte[]> inputIds = new ArrayList<byte[]>();
            inputIds.add(carSellOrder.id());

            CarBoxData carBoxData = new CarBoxData(carSellOrder.proposition(), 1,
                    carSellOrder.getBoxData().getVin(), carSellOrder.getBoxData().getYear(),
                    carSellOrder.getBoxData().getModel(), carSellOrder.getBoxData().getColor(),
                    carSellOrder.getBoxData().getDescription());

            List<Proof<Proposition>> fakeProofs = Collections.nCopies(inputIds.size(), null);

            CarSellTransaction unsignedTransaction = new CarSellTransaction(inputIds, carSellOrder.getBoxData(),
                    carBoxData, Optional.empty(), Optional.empty(), fakeProofs, fee, timestamp,
                    sidechainBoxesDataCompanion, sidechainProofsCompanion);

            byte[] messageToSign = unsignedTransaction.messageToSign();

            List<Proof<Proposition>> proofs = new ArrayList<>();

            proofs.add(view.getNodeWallet().secretByPublicKey(carSellOrder.proposition()).get().sign(messageToSign));

            CarSellTransaction transaction = new CarSellTransaction(inputIds, carSellOrder.getBoxData(),
                    carBoxData, Optional.empty(), Optional.empty(), proofs, fee, timestamp,
                    sidechainBoxesDataCompanion, sidechainProofsCompanion);

            return new CancelCarSellOrderResponce(ByteUtils.toHexString(sidechainTransactionsCompanion.toBytes((BoxTransaction) transaction)));
        } catch (Exception e) {
            return new CarResponseError("0103", "Error.", Some.apply(e));
        }*/
    }

    @JsonView(Views.Default.class)
    class CarResponse implements SuccessResponse {
        public String transactionBytes;

        public CarResponse(String transactionBytes) {
            this.transactionBytes = transactionBytes;
        }
    }

    public static class CreateCarSellOrderRequest {
        String carBoxId;
        long sellPrice;
        long fee;

        public String getCarBoxId() {
            return carBoxId;
        }

        public void setCarBoxId(String carBoxId) {
            this.carBoxId = carBoxId;
        }

        public long getSellPrice() {
            return sellPrice;
        }

        public void setSellPrice(long sellPrice) {
            this.sellPrice = sellPrice;
        }

        public long getFee() {
            return fee;
        }

        public void setFee(int fee) {
            this.fee = fee;
        }
    }

    @JsonView(Views.Default.class)
    class CreateCarSellOrderResponce implements SuccessResponse {
        private final String carSellOrderTxBytes;

        public CreateCarSellOrderResponce(String carSellOrderTxBytes) {
            this.carSellOrderTxBytes = carSellOrderTxBytes;
        }

        public String carSellOrderTxBytes() {
            return carSellOrderTxBytes;
        }

        public String getCarSellOrderTxBytes() {
            return carSellOrderTxBytes;
        }
    }

    public static class AcceptCarSellOrderRequest {
        String carSellOrderId;
        PublicKey25519Proposition buyerProposition;

        public String getCarSellOrderId() {
            return carSellOrderId;
        }

        public void setCarSellOrderId(String carSellOrderId) {
            this.carSellOrderId = carSellOrderId;
        }

        public PublicKey25519Proposition getBuyerProposition() {
            return buyerProposition;
        }

        public void setBuyerProposition(String propositionHexBytes) {
            byte[] propositionBytes = BytesUtils.fromHexString(propositionHexBytes);
            buyerProposition = new PublicKey25519Proposition(propositionBytes);
        }

    }

    public static class CancelCarSellOrderRequest {
        String carSellOrderId;

        public String getCarSellOrderId() {
            return carSellOrderId;
        }

        public void setCarSellOrderId(String carSellOrderId) {
            this.carSellOrderId = carSellOrderId;
        }
    }

    @JsonView(Views.Default.class)
    class AcceptCarSellOrderResponce implements SuccessResponse {
        private final String acceptedCarSellOrderTxBytes;

        public AcceptCarSellOrderResponce(String acceptedCarSellOrderTxBytes) {
            this.acceptedCarSellOrderTxBytes = acceptedCarSellOrderTxBytes;
        }

        public String acceptedCarSellOrderTxBytes() {
            return acceptedCarSellOrderTxBytes;
        }

        public String getAcceptedCarSellOrderTxBytes() {
            return acceptedCarSellOrderTxBytes;
        }
    }

    @JsonView(Views.Default.class)
    class CancelCarSellOrderResponce implements SuccessResponse {
        private final String canceledCarSellOrderTxBytes;

        public CancelCarSellOrderResponce(String canceledCarSellOrderTxBytes) {
            this.canceledCarSellOrderTxBytes = canceledCarSellOrderTxBytes;
        }

        public String canceledCarSellOrderTxBytes() {
            return canceledCarSellOrderTxBytes;
        }

        public String getCanceledCarSellOrderTxBytes() {
            return canceledCarSellOrderTxBytes;
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
        public String code() {
            return null;
        }

        @Override
        public String description() {
            return null;
        }

        @Override
        public Option<Throwable> exception() {
            return null;
        }
    }

    private List<byte[]> boxesFromMempool(NodeMemoryPool mempool) {
        List<byte[]> boxesFromMempool = new ArrayList<>();
        for(BoxTransaction tx : mempool.getTransactions()) {
            Set<ByteArrayWrapper> ids = tx.boxIdsToOpen();
            for(ByteArrayWrapper id : ids) {
                boxesFromMempool.add(id.data());
            }
        }
        return boxesFromMempool;
    }
}

