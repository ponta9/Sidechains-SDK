package com.horizen.examples.car;

import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonView;
import com.horizen.api.http.ApiResponse;
import com.horizen.api.http.ApplicationApiGroup;
import com.horizen.api.http.SuccessResponse;
import com.horizen.node.SidechainNodeView;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.serialization.Views;
import com.horizen.utils.BytesUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

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
    CarBoxData carBoxData = new CarBoxData(ent.proposition, 1, ent.vin);
    return new CarResponse();
  }

  public static class CreateCarBoxRequest {
    BigInteger vin;
    PublicKey25519Proposition proposition;

    public BigInteger getVin()
    {
      return vin;
    }

    public void setVin(String vin)
    {
      this.vin = new BigInteger(vin);
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
  }

  @JsonView(Views.CustomView.class)
  class CarResponse implements SuccessResponse{
  }
}

