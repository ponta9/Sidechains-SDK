package com.horizen.api.http.schema

import com.fasterxml.jackson.annotation.JsonView
import com.horizen.serialization.Views

/**
  * Set of classes representing input and output format of all REST Api requests about block operations.
  *
  * Naming convection used (in camel case).
  * - For REST request: 'Req' + resource path + method
  * - For REST response: 'Resp' + resource path + method
  *
  * Example
  *  We have an Api with group name 'myGroup' and two resources path 'path_1' and 'path_2'.
  *  The full uri path will be:
  *
  *  1) http://host:port/myGroup/path_1
  *  2) http://host:port/myGroup/path_2
  *
  *  Classes implemented will be (assumed the HTTP method used is POST for all resources):
  *  1)
  *     1.1) class ReqPath_1Post
  *     1.2) class RespPath_1Post
  *  2)
  *     2.1) class ReqPath_2Post
  *     2.2) class RespPath_2Post
  *
  * Note:
  * In case of requests/responses with the same inputs/outputs format, an unique class will be implemented, without following the above naming convection.
  */

object SidechainNodeRestSchema {

  @JsonView(Array(classOf[Views.Default]))
  case class SidechainPeerNode(address: String, lastSeen: Long, name: String, connectionType: Option[String])

  @JsonView(Array(classOf[Views.Default]))
  case class RespBlacklistedPeersPost(addresses: Seq[String])

  @JsonView(Array(classOf[Views.Default]))
  case class ReqConnectPost(host : String, port : Int)

  @JsonView(Array(classOf[Views.Default]))
  case class RespConnectPost(connectedTo : String)

}
