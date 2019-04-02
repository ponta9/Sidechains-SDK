package com.horizen

import java.util.{Optional, List => JList, Map => JMap}

import com.horizen.block.SidechainBlock
import com.horizen.box.Box
import com.horizen.wallet.ApplicationWallet
import com.horizen.node.NodeWallet
import com.horizen.proposition.Proposition
import com.horizen.proposition.ProofOfKnowledgeProposition
import com.horizen.secret.Secret
import com.horizen.storage.{SidechainSecretStorage, SidechainWalletBoxStorage}
import com.horizen.transaction.{BoxTransaction, Transaction}
import com.horizen.utils.ByteArrayWrapper
import scorex.core.VersionTag
import scorex.util.{bytesToId, idToBytes}

import scala.collection.immutable
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._


// 2 stores: one for Boxes(WalletBoxes), another for secrets
// TO DO: we need to wrap LSMStore

// TO DO: put also SidechainSecretsCompanion and SidechainBoxesCompanion with a data provided by Sidechain developer

trait Wallet[S <: Secret, P <: Proposition, TX <: Transaction, PMOD <: scorex.core.PersistentNodeViewModifier, W <: Wallet[S, P, TX, PMOD, W]]
  extends scorex.core.transaction.wallet.Vault[TX, PMOD, W] {
  self: W =>

  def addSecret(secret: S): Try[W]

  def removeSecret(publicImage: P): Try[W]

  def secret(publicImage: P): Option[S]

  def secrets(): Set[S]

  def boxes(): Seq[WalletBox]

  def publicKeys(): Set[P]
}

class SidechainWallet(seed: Array[Byte], walletBoxStorage: SidechainWalletBoxStorage, secretStorage: SidechainSecretStorage)
                     (applicationWallet: ApplicationWallet)
  extends Wallet[Secret,
                 SidechainTypes#P,
                 SidechainTypes#BT,
                 SidechainBlock,
                 SidechainWallet]
  with NodeWallet
{
  override type NVCT = SidechainWallet

  // 1) check for existence
  // 2) try to store in SecretStoreusing SidechainSecretsCompanion
  override def addSecret(secret: Secret): Try[SidechainWallet] = Try {
    secretStorage.update(secret)
    if (applicationWallet != null)
      applicationWallet.onAddSecret(secret)
    this
  }

  // 1) check for existence
  // 2) remove from SecretStore (note: provide a unique version to SecretStore)
  override def removeSecret(publicImage: SidechainTypes#P): Try[SidechainWallet] = Try {
      secretStorage.remove(publicImage)
      if (applicationWallet != null)
        applicationWallet.onRemoveSecret(publicImage)
      this
  }

  override def secret(publicImage: SidechainTypes#P): Option[Secret] = {
    secretStorage.get(publicImage)
  }

  // get all secrets, use SidechainSecretsCompanion to deserialize
  override def secrets(): immutable.Set[Secret] = {
    secretStorage.getAll.toSet
  }

  // get all boxes as WalletBox object using SidechainBoxesCompanion
  override def boxes(): immutable.Seq[WalletBox] = {
    walletBoxStorage.getAll.toSeq
  }

  // get all secrets using SidechainSecretsCompanion -> get .publicImage of each
  override def publicKeys(): immutable.Set[SidechainTypes#P] = {
    secretStorage.getAll.map(_.publicImage().asInstanceOf[SidechainTypes#P]).toSet
  }

  // just do nothing, we don't need to care about offchain objects inside the wallet
  override def scanOffchain(tx: SidechainTypes#BT): SidechainWallet = this

  // just do nothing, we don't need to care about offchain objects inside the wallet
  override def scanOffchain(txs: Seq[SidechainTypes#BT]): SidechainWallet = this

  // scan like in HybridApp, but in more general way.
  // update boxes in BoxStore
  override def scanPersistent(modifier: SidechainBlock): SidechainWallet = {
    val changes = SidechainState.changes(modifier).get

    val newBoxes = changes.toAppend.filter(s => publicKeys().contains(s.box.proposition().asInstanceOf[SidechainTypes#P]))
        .map(_.box)
        .map { box =>
               val boxTransaction = modifier.transactions.find(t => t.newBoxes().asScala.exists(tb => java.util.Arrays.equals(tb.id, box.id)))
               val txId : Array[Byte]= boxTransaction.map(_.id).get.getBytes
               val ts = boxTransaction.map(_.timestamp).getOrElse(modifier.timestamp)
               WalletBox(box.asInstanceOf[SidechainTypes#B], txId, ts)
    }

    val boxIdsToRemove = changes.toRemove.map(_.boxId).map(new ByteArrayWrapper(_))
    walletBoxStorage.update(modifier.id.getBytes, newBoxes.toList, boxIdsToRemove.map(_.data).toList)

    if (applicationWallet != null)
      applicationWallet.onChangeBox(newBoxes.map(_.box.asInstanceOf[Box[_ <: Proposition]]).toList.asJava,
        boxIdsToRemove.map(_.data).toList.asJava)

    this
  }

  // rollback BoxStore only. SecretStore must not changed
  override def rollback(to: VersionTag): Try[SidechainWallet] = Try {
    walletBoxStorage.rollback(new ByteArrayWrapper(to.getBytes))

    this
  }

  // Java NodeWallet interface definition
  override def allBoxes : JList[Box[_ <: Proposition]] = {
    walletBoxStorage.getAll.map(_.box.asInstanceOf[Box[_ <: Proposition]]).toList.asJava
  }

  override def allBoxes(boxIdsToExclude: JList[Array[Byte]]): JList[Box[_ <: Proposition]] = {
    walletBoxStorage.getAll.filter((wb : WalletBox) => !boxIdsToExclude.contains(wb.box.id()))
      .map(_.box.asInstanceOf[Box[_ <: Proposition]])
      .asJava
  }

  override def boxesOfType(boxType: Class[_ <: Box[_ <: Proposition]]): JList[Box[_ <: Proposition]] = {
    walletBoxStorage.getByType(boxType)
      .map(_.box.asInstanceOf[Box[_ <: Proposition]])
      .asJava
  }

  override def boxesOfType(boxType: Class[_ <: Box[_ <: Proposition]], boxIdsToExclude: JList[Array[Byte]]): JList[Box[_ <: Proposition]] = {
    walletBoxStorage.getByType(boxType)
      .map(_.box.asInstanceOf[Box[_ <: Proposition]])
      .filter(box => !boxIdsToExclude.contains(box.id()))
      .asJava
  }

  override def boxAmount(boxType: Class[_ <: Box[_ <: Proposition]]): java.lang.Long = {
    walletBoxStorage.getBoxAmount(boxType)
  }

  override def secretByPublicImage(publicImage: ProofOfKnowledgeProposition[_ <: Secret]): Secret = {
    secretStorage.get(publicImage).get
  }

  override def allSecrets(): JList[Secret] = {
    secretStorage.getAll.asJava
  }

  override def secretsOfType(secretType: Class[_ <: Secret]): JList[Secret] = {
    secretStorage.getAll.filter(_.getClass.equals(secretType)).asJava
  }

}
