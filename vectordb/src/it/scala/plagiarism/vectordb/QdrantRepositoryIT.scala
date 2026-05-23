package plagiarism.vectordb

import com.dimafeng.testcontainers.QdrantContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import plagiarism.testutils.TestFixtures

class QdrantRepositoryIT extends AnyFunSuite with Matchers with TestContainerForAll:

  override val containerDef: QdrantContainer.Def = QdrantContainer.Def()

  private val vectorDim = TestFixtures.vectorDimension

  test("createCollectionIfNotExists should create a new collection") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_create_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        val size = repo.collectionSize(collection)
        size shouldBe 0
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }

  test("createCollectionIfNotExists should not fail if collection already exists") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_idempotent_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        noException should be thrownBy
          repo.createCollectionIfNotExists(collection, vectorDim)
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }

  test("upsert should insert a point that can be counted") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_upsert_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        val vector = TestFixtures.fakeEmbedding(seed = 1L)
        repo.upsert(collection, pointId = 1L, vector, Map("student" -> "studentA", "file_path" -> "main.c"))
        Thread.sleep(500)
        repo.collectionSize(collection) shouldBe 1
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }

  test("search should return the most similar vector") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_search_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        val v1 = TestFixtures.fakeEmbedding(seed = 1L)
        val v2 = TestFixtures.fakeEmbedding(seed = 2L)
        repo.upsert(collection, 1L, v1, Map("student" -> "studentA", "file_path" -> "a.c"))
        repo.upsert(collection, 2L, v2, Map("student" -> "studentB", "file_path" -> "b.c"))
        Thread.sleep(500)
        val results = repo.search(collection, v1, limit = 2)
        results should have length 2
        results.head.payload("student") shouldBe "studentA"
        results.head.score should be > results(1).score
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }

  test("search should respect the limit parameter") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_limit_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        for i <- 1 to 5 do
          repo.upsert(
            collection,
            i.toLong,
            TestFixtures.fakeEmbedding(i.toLong),
            Map("student" -> s"student$i", "file_path" -> "main.c")
          )
        Thread.sleep(500)
        val results = repo.search(collection, TestFixtures.fakeEmbedding(1L), limit = 3)
        results should have length 3
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }

  test("upsertBatch should insert multiple points at once") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_batch_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        val points = (1 to 10).map { i =>
          (i.toLong, TestFixtures.fakeEmbedding(i.toLong), Map("student" -> s"student$i", "file_path" -> "main.c"))
        }.toList
        repo.upsertBatch(collection, points)
        Thread.sleep(500)
        repo.collectionSize(collection) shouldBe 10
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }

  test("search results should include payload data") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_payload_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        val payload =
          Map("student" -> "studentA", "file_path" -> "solution/src/main.c", "diff_text" -> "test diff content here")
        repo.upsert(collection, 1L, TestFixtures.fakeEmbedding(1L), payload)
        Thread.sleep(500)
        val results = repo.search(collection, TestFixtures.fakeEmbedding(1L), limit = 1)
        results should have length 1
        results.head.payload("student") shouldBe "studentA"
        results.head.payload("file_path") shouldBe "solution/src/main.c"
        results.head.payload("diff_text") shouldBe "test diff content here"
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }

  test("deleteCollection should remove the collection") {
    withContainers { qdrant =>
      val repo       = new QdrantRepository(qdrant.container.getHost, qdrant.grpcPort)
      val collection = s"test_delete_${System.nanoTime()}"
      try
        repo.createCollectionIfNotExists(collection, vectorDim)
        repo.upsert(collection, 1L, TestFixtures.fakeEmbedding(1L), Map("student" -> "x", "file_path" -> "x"))
        repo.deleteCollection(collection)
        repo.createCollectionIfNotExists(collection, vectorDim)
        repo.collectionSize(collection) shouldBe 0
      finally
        try repo.deleteCollection(collection)
        catch case _: Exception => ()
        repo.close()
    }
  }
