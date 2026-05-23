package plagiarism.vectordb

import io.qdrant.client.PointIdFactory.id
import io.qdrant.client.ValueFactory.value
import io.qdrant.client.VectorsFactory.vectors
import io.qdrant.client.grpc.Collections.Distance
import io.qdrant.client.grpc.Collections.VectorParams
import io.qdrant.client.grpc.Points.PointStruct
import io.qdrant.client.grpc.Points.SearchPoints
import io.qdrant.client.grpc.Points.WithPayloadSelector
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import org.slf4j.LoggerFactory

import java.util.HashMap as JHashMap
import java.util.List as JList
import scala.jdk.CollectionConverters.*

class QdrantRepository(host: String, grpcPort: Int):

  private val logger = LoggerFactory.getLogger(getClass)

  private val resolvedHost: String =
    if (host == "localhost") "127.0.0.1" else host

  private val client: QdrantClient = new QdrantClient(
    QdrantGrpcClient.newBuilder(resolvedHost, grpcPort, false).build()
  )

  def createCollectionIfNotExists(collectionName: String, vectorSize: Int): Unit =
    val collections = client.listCollectionsAsync().get().asScala
    if (!collections.contains(collectionName))
      client
        .createCollectionAsync(
          collectionName,
          VectorParams
            .newBuilder()
            .setDistance(Distance.Cosine)
            .setSize(vectorSize)
            .build()
        )
        .get()

  def upsert(
      collectionName: String,
      pointId: Long,
      vector: Array[Float],
      payload: Map[String, String]
  ): Unit =
    val payloadMap = new JHashMap[String, io.qdrant.client.grpc.JsonWithInt.Value]()
    payload.foreach { case (k, v) => payloadMap.put(k, value(v)) }

    val point = PointStruct
      .newBuilder()
      .setId(id(pointId))
      .setVectors(vectors(vector*))
      .putAllPayload(payloadMap)
      .build()

    client.upsertAsync(collectionName, JList.of(point)).get()

  def upsertBatch(
      collectionName: String,
      points: List[(Long, Array[Float], Map[String, String])]
  ): Unit =
    val pointStructs = points.map { case (pid, vec, payload) =>
      val payloadMap = new JHashMap[String, io.qdrant.client.grpc.JsonWithInt.Value]()
      payload.foreach { case (k, v) => payloadMap.put(k, value(v)) }
      PointStruct
        .newBuilder()
        .setId(id(pid))
        .setVectors(vectors(vec*))
        .putAllPayload(payloadMap)
        .build()
    }
    client.upsertAsync(collectionName, pointStructs.asJava).get()

  def search(
      collectionName: String,
      queryVector: Array[Float],
      limit: Int = 5
  ): List[SearchResult] =
    val queryVec     = queryVector.toList.map(Float.box).asJava
    val searchPoints = SearchPoints
      .newBuilder()
      .setCollectionName(collectionName)
      .addAllVector(queryVec)
      .setLimit(limit)
      .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
      .build()

    val scored = client.searchAsync(searchPoints).get()

    scored.asScala.map { s =>
      val payload = s.getPayloadMap.asScala.map { case (k, v) =>
        k -> v.getStringValue
      }.toMap
      SearchResult(
        id = s.getId.getNum,
        score = s.getScore,
        payload = payload
      )
    }.toList

  def collectionSize(collectionName: String): Long =
    client.getCollectionInfoAsync(collectionName).get().getPointsCount

  def deleteCollection(collectionName: String): Unit =
    client.deleteCollectionAsync(collectionName).get()

  def close(): Unit =
    client.close()

case class SearchResult(
    id: Long,
    score: Float,
    payload: Map[String, String]
)
