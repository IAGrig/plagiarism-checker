package plagiarism.vectordb

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import plagiarism.core.FileDiff
import plagiarism.testutils.QdrantTestContainer
import plagiarism.testutils.TestFixtures

class SimilaritySearchIT extends AnyFunSuite with Matchers with QdrantTestContainer with BeforeAndAfterAll:

  private val collectionName = "similarity_test"

  private var embeddingService: EmbeddingService = _

  private var qdrantRepo: QdrantRepository = _

  private var searchService: SimilaritySearchService = _

  override def beforeAll(): Unit =
    super.beforeAll()
    embeddingService = EmbeddingService.create()

  override def afterAll(): Unit =
    if (embeddingService != null) embeddingService.close()
    super.afterAll()

  test("full pipeline: populate submissions then query for similar code") {
    withContainers { qdrant =>
      qdrantRepo = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      searchService = new SimilaritySearchService(embeddingService, qdrantRepo, collectionName)

      qdrantRepo.createCollectionIfNotExists(collectionName, embeddingService.dimension)

      val studentADiffs = List(
        FileDiff("solution/src/main.c", TestFixtures.studentADiff)
      )
      searchService.upsertSubmission("studentA", studentADiffs, startId = 0L)

      val studentCDiffs = List(
        FileDiff("solution/src/main.c", TestFixtures.studentCDiff)
      )
      searchService.upsertSubmission("studentC", studentCDiffs, startId = 100L)

      val unrelatedDiffs = List(
        FileDiff("src/main.rs", TestFixtures.unrelatedDiff)
      )
      searchService.upsertSubmission("unrelated", unrelatedDiffs, startId = 200L)

      val results = searchService.searchSimilar(TestFixtures.plagiarismDIff, topK = 3)

      results should not be empty
      results.head.studentName shouldBe "studentA"
      results.head.score should be > 0.9f

      val studentCResult = results.find(_.studentName == "studentC")
      studentCResult shouldBe defined
      studentCResult.get.score should be < results.head.score

      qdrantRepo.deleteCollection(collectionName)
      qdrantRepo.close()
    }
  }

  test("checkDiff should parse raw diff input and find matches") {
    withContainers { qdrant =>
      val collection2 = "check_diff_test"
      qdrantRepo = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      searchService = new SimilaritySearchService(embeddingService, qdrantRepo, collection2)

      qdrantRepo.createCollectionIfNotExists(collection2, embeddingService.dimension)

      val studentADiffs = List(
        FileDiff("solution/src/main.c", TestFixtures.studentADiff)
      )
      searchService.upsertSubmission("studentA", studentADiffs, startId = 0L)

      val results = searchService.checkDiff(TestFixtures.plagiarismDIff, topK = 5)

      results should not be empty

      qdrantRepo.deleteCollection(collection2)
      qdrantRepo.close()
    }
  }

  test("searching with unrelated code should rank matches low") {
    withContainers { qdrant =>
      val collection3 = "unrelated_test"
      qdrantRepo = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      searchService = new SimilaritySearchService(embeddingService, qdrantRepo, collection3)

      qdrantRepo.createCollectionIfNotExists(collection3, embeddingService.dimension)

      searchService.upsertSubmission(
        "studentA",
        List(FileDiff("solution/src/main.c", TestFixtures.studentADiff)),
        startId = 0L
      )

      val results = searchService.searchSimilar(TestFixtures.unrelatedDiff, topK = 5)

      results should not be empty
      results.head.score should be < 0.85f

      qdrantRepo.deleteCollection(collection3)
      qdrantRepo.close()
    }
  }

  test("empty database search should return empty results") {
    withContainers { qdrant =>
      val collection4 = "empty_test"
      qdrantRepo = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      searchService = new SimilaritySearchService(embeddingService, qdrantRepo, collection4)

      qdrantRepo.createCollectionIfNotExists(collection4, embeddingService.dimension)

      val results = searchService.searchSimilar("some code", topK = 5)
      results shouldBe empty

      qdrantRepo.deleteCollection(collection4)
      qdrantRepo.close()
    }
  }
