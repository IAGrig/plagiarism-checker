package plagiarism.web

import org.slf4j.LoggerFactory
import plagiarism.vectordb.EmbeddingService
import plagiarism.vectordb.QdrantRepository
import plagiarism.vectordb.SimilaritySearchService

// sbt "webApp/runMain plagiarism.web.appServer [qdrantHost] [qdrantPort] [httpPort]"
@main def appServer(
    qdrantHost: String = "localhost",
    qdrantGrpcPort: Int = 6334,
    httpPort: Int = 8080
): Unit =
  val logger         = LoggerFactory.getLogger("AppServer")
  val collectionName = "submissions"

  val embeddingService = EmbeddingService.create()
  val qdrantRepo       = new QdrantRepository(qdrantHost, qdrantGrpcPort)
  val searchService    = new SimilaritySearchService(embeddingService, qdrantRepo, collectionName)

  val routes = new Routes(searchService, "0.0.0.0", httpPort)

  routes.main(Array.empty)

  logger.info(s"Server started at http://localhost:$httpPort/")
