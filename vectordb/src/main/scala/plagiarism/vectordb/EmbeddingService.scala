package plagiarism.vectordb

import ai.djl.Application
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory
import ai.djl.repository.zoo.Criteria
import ai.djl.repository.zoo.ZooModel
import ai.djl.training.util.ProgressBar
import org.slf4j.LoggerFactory

import java.util.ArrayList
import scala.jdk.CollectionConverters.*

class EmbeddingService private (model: ZooModel[String, Array[Float]]):

  private val logger = LoggerFactory.getLogger(getClass)

  val dimension: Int = 384

  def embed(text: String): Array[Float] =
    val predictor = model.newPredictor()
    try
      predictor.predict(text)
    finally
      predictor.close()

  def embedBatch(texts: Seq[String]): Seq[Array[Float]] =
    val predictor = model.newPredictor()
    try
      val input = ArrayList[String]()
      texts.foreach(input.add)
      predictor.batchPredict(input).asScala.toSeq
    finally predictor.close()

  def close(): Unit =
    logger.info("Closing embedding model resources")
    model.close()

object EmbeddingService:

  private val logger = LoggerFactory.getLogger(getClass)

  val DefaultModelUrl: String =
    "djl://ai.djl.huggingface.pytorch/BAAI/bge-small-en-v1.5"

  def create(modelUrl: String = DefaultModelUrl): EmbeddingService =
    logger.info(s"Loading embedding model from $modelUrl")
    val criteria = Criteria
      .builder()
      .optApplication(Application.NLP.TEXT_EMBEDDING)
      .setTypes(classOf[String], classOf[Array[Float]])
      .optModelUrls(modelUrl)
      .optEngine("PyTorch")
      .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
      .optProgress(new ProgressBar())
      .build()
    val model = criteria.loadModel()
    logger.info("Loaded embedding model: ", model.getName)
    new EmbeddingService(model)
