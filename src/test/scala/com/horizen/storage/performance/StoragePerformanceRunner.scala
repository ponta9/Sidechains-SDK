package com.horizen.storage.performance

import java.io.{PrintWriter, StringWriter}

import com.horizen.fixtures.{IODBStoreFixture, TransactionFixture}
import com.horizen.storage.performance.Measure._
import com.horizen.storage.{ErgoPlatformStorageAdapter, Storage}
import com.horizen.utils._
import javafx.util.{Pair => JPair}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.reflect.io.File
import scala.util.Random

abstract case class StoragePerformanceTestConfig(storageCreationUpdatingBatchSize: Int,
                                                 measureIterationsCount: Int,
                                                 readingBatchingSizesInKb: Seq[Int],
                                                 writingBatchingSizesInKb: Seq[Int]){

  def storageGenerationSettings: Seq[(Int, StorageDataGenerator)]
}

case class StorageData(storage: Storage,
                       availableKeys: Seq[ByteArrayWrapper],
                       storageSize: Long,
                       storageDataGenerator: StorageDataGenerator) {
  def storageElementsSize: Int = availableKeys.size

  def getKeysToRead(requiredSize: Int): Seq[ByteArrayWrapper] = {
    require(requiredSize <= storageElementsSize)
    Random.shuffle(availableKeys).take(requiredSize)
  }

  override def toString: String = {
    s"Storage with ${storageElementsSize} available keys; Total size is ${storageSize / (1024 * 1024)}MB; Average one element size is ${(storageSize / 1024) / storageElementsSize}KB"
  }
}

class StoragePerformanceRunner(config: StoragePerformanceTestConfig, debug: Boolean = true) extends TransactionFixture with IODBStoreFixture{

  def measurePerformance(): Stream[(String, Seq[TimeMeasure])] = {
    config.storageGenerationSettings.toStream.map{
      case (size, dataGenerator) =>
        val allMeasures = mutable.Buffer[TimeMeasure]()
        val storagePath = System.getProperty("java.io.tmpdir") + "StorageTest_" + System.currentTimeMillis()
        val storage: Storage = new ErgoPlatformStorageAdapter(storagePath)
        var storageDescription: String = ""

        try {
          if (debug) println(s"CREATION next storage with size ${size}")
          val (storageData, storageCreationTimeMeasures) = fillAndMeasureStorage(storage, dataGenerator, size)
          allMeasures.appendAll(storageCreationTimeMeasures)

          if (debug) println(s"Start checking read once")
          val readingOnceMeasure =
            (1 to 3).map(_ => getStatisticForReading(storageData, readFromStorageOnce))
          allMeasures.appendAll(readingOnceMeasure)

          if (debug) println(s"Start checking read batch")
          val readingBatchMeasure =
            config.readingBatchingSizesInKb.map(batchingSizeInKb => getStatisticForReading(storageData, readFromStorageBatching(batchingSizeInKb * 1024)))
          allMeasures.appendAll(readingBatchMeasure)

          if (debug) println(s"Start checking write batch")
          val writingBatchMeasure =
            config.writingBatchingSizesInKb.map(batchingSizeInKb => getStatisticForWriting(storageData, storageData.storageDataGenerator.copyGenerator(), batchingSizeInKb * 1024))
          allMeasures.appendAll(writingBatchMeasure)

          storageDescription = storageData.toString
        }
        catch {
          case e: Exception => {
            if (debug) println(s"Got next exception during test:")
            val sw = new StringWriter
            e.printStackTrace(new PrintWriter(sw))
            storageDescription = sw.toString
          }
        }
        finally {
          cleanUpStorage(storage, storagePath)
        }
        (storageDescription, allMeasures)
    }
  }

  private def fillAndMeasureStorage(storage: Storage, dataGenerator: StorageDataGenerator, requiredTransactionsCount: Int): (StorageData, Seq[TimeMeasure]) = {
    val dataCountSizeForStorageCreationMeasure: Int = requiredTransactionsCount / 33
    val usedIds: mutable.Buffer[ByteArrayWrapper] = mutable.Buffer[ByteArrayWrapper]()

    var totalWrittenTransactionsCounter: Long = 0
    var totalWrittenTransactionsSize: Long = 0

    var currentWrittenTransactionSize: Long = 0

    var timeMeasures = Seq[TimeMeasure]()
    var currentTimeMeasure = TimeMeasure(StorageCreation, 0, 0, 0, Long.MaxValue, 0, totalWrittenTransactionsCounter)

    val toStorageBuffer = new java.util.LinkedList[JPair[ByteArrayWrapper, ByteArrayWrapper]]()
    dataGenerator.buildGeneratedDataStream().take(requiredTransactionsCount).foreach{dataToAdd =>
      toStorageBuffer.addLast(dataToAdd)

      if (((totalWrittenTransactionsCounter % dataCountSizeForStorageCreationMeasure) == 0) && (currentTimeMeasure.itemsCount > 0)) {
        if (debug) println(s"${totalWrittenTransactionsCounter} transactions had been written to the storage")

        timeMeasures = timeMeasures :+ currentTimeMeasure
        currentTimeMeasure = TimeMeasure(StorageCreation, 0, 0, 0, Long.MaxValue, 0, totalWrittenTransactionsCounter)
      }

      totalWrittenTransactionsCounter += 1
      totalWrittenTransactionsSize += dataToAdd.getValue.length
      currentWrittenTransactionSize += dataToAdd.getValue.length

      if ((config.storageCreationUpdatingBatchSize < currentWrittenTransactionSize) || (totalWrittenTransactionsCounter == requiredTransactionsCount)) {
        val newVer = getVersion
        val startTime = System.nanoTime()
        storage.update(newVer, toStorageBuffer, Seq().asJava)
        val updateTime = System.nanoTime() - startTime
        val totalSize = toStorageBuffer.stream().mapToInt(_.getValue.size).sum()

        currentTimeMeasure = currentTimeMeasure + TimeMeasure(StorageCreation, updateTime, toStorageBuffer.size(), totalSize, totalSize, totalSize, totalWrittenTransactionsCounter)

        usedIds.appendAll(toStorageBuffer.asScala.map(_.getKey))

        toStorageBuffer.clear()
        currentWrittenTransactionSize = 0
      }
    }

    timeMeasures = timeMeasures :+ currentTimeMeasure
    (StorageData(storage, usedIds, totalWrittenTransactionsSize, dataGenerator), timeMeasures)
  }

  private def getStatisticForReading(storageData: StorageData, readFunction: StorageData => TimeMeasure): TimeMeasure = {
    val measures = (1 to config.measureIterationsCount).map{ _ =>
      readFunction(storageData)
    }.toList
    TimeMeasure.measureStatistic(measures)
  }

  private def readFromStorageOnce(storageData: StorageData): TimeMeasure = {
    val toRead: Seq[ByteArrayWrapper] = storageData.getKeysToRead(1)
    val storage = storageData.storage
    val startTime = System.nanoTime()
    val receivedData = toRead.map{d => storage.get(d)}
    val endTime = System.nanoTime()
    val totalTime = endTime - startTime
    require(receivedData.forall(_.isPresent))
    val totalSize = receivedData.map(_.get().length).sum

    TimeMeasure(StorageReadOnce, totalTime, toRead.size, totalSize, totalSize, totalSize, storageData.storageElementsSize)
  }

  private def readFromStorageBatching(batchingSizeInBytes: Int)(storageData: StorageData): TimeMeasure = {
    val batchingSize = Math.max(1, batchingSizeInBytes / storageData.storageDataGenerator.expectedAverageDataLen)
    val generatedReadKeys = storageData.getKeysToRead(batchingSize).asJava

    var totalTime: Long = 0
    val storage = storageData.storage

    val startTime = System.nanoTime()
    val received = storage.get(generatedReadKeys)
    val endTime = System.nanoTime()
    val spentTime = endTime - startTime
    totalTime = totalTime + spentTime

    val receivedDataSize =  received.stream().mapToInt(d => d.getValue.get().size).sum

    val readingMeasure = TimeMeasure(StorageReadBatching,
      totalTime,
      received.size,
      receivedDataSize,
      receivedDataSize,
      receivedDataSize,
      storageData.storageElementsSize)

    readingMeasure
  }

  private def writeAndMeasure(storageData: StorageData, dataStream: Iterator[JPair[ByteArrayWrapper, ByteArrayWrapper]], batchSizeInBytes: Int, rollback: Boolean = true): TimeMeasure = {
    val emptyRemove: java.util.List[ByteArrayWrapper] = Seq().asJava
    val storage = storageData.storage
    val versionForRollback = storage.lastVersionID()

    val toWrite = new java.util.ArrayList[JPair[ByteArrayWrapper, ByteArrayWrapper]]()
    var generatedDataSize = 0
    while (generatedDataSize < batchSizeInBytes) {
      val dataToAppend = dataStream.next()
      toWrite.add(dataToAppend)
      generatedDataSize += dataToAppend.getValue.length
    }

    val version = getRandomByteArrayWrapper(32)
    val startTime = System.nanoTime()
    storage.update(version, toWrite, emptyRemove)
    val endTime = System.nanoTime()

    val rollbacks = storage.rollbackVersions()
    if (rollback) {
      if (rollbacks.contains(versionForRollback.get)) {
        storage.rollback(versionForRollback.get)
        val allUsedKeys = toWrite.asScala.map(_.getValue)
        require(allUsedKeys.forall(!storage.get(_).isPresent))
      }
      else {
        if (debug) println("Failed to perform rollback")
      }
    }
    val totalTime = endTime - startTime

    TimeMeasure(StorageWriteBatching,
      totalTime,
      toWrite.size(),
      generatedDataSize,
      generatedDataSize,
      generatedDataSize,
      storageData.storageElementsSize)
  }

  private def getStatisticForWriting(storageData: StorageData, dataGenerator: StorageDataGenerator, batchSize: Int) = {
    val dataStream: Iterator[JPair[ByteArrayWrapper, ByteArrayWrapper]] = dataGenerator.buildGeneratedDataStream().iterator
    val allMeasures = (1 to config.measureIterationsCount).map{ _ =>
      writeAndMeasure(storageData, dataStream, batchSize)
    }
    TimeMeasure.measureStatistic(allMeasures)
  }

  private def grouping(groupSize: Int)(data: Seq[JPair[ByteArrayWrapper, ByteArrayWrapper]]) = {
    data.grouped(groupSize).withFilter(g => g.size == groupSize).map(d => (getRandomByteArrayWrapper(32), d.asJava)).toList.asJava
  }

  private def getRandomByteArrayWrapper(length: Int): ByteArrayWrapper = {
    val generatedData: Array[Byte] = new Array[Byte](length)
    util.Random.nextBytes(generatedData)
    generatedData
  }

  private def cleanUpStorage(storage: Storage, storagePath: String): Unit = {
    storage.close()
    val file = File(storagePath)
    file.deleteRecursively()
  }
}
