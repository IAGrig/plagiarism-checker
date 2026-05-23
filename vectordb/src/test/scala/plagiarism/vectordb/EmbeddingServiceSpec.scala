package plagiarism.vectordb

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import plagiarism.testutils.TestFixtures

class EmbeddingServiceSpec extends AnyFunSuite with Matchers with BeforeAndAfterAll:

  private var service: EmbeddingService = _

  override def beforeAll(): Unit =
    super.beforeAll()
    service = EmbeddingService.create()

  override def afterAll(): Unit =
    if (service != null) service.close()
    super.afterAll()

  test("embed should return a vector of the correct dimension") {
    val embedding = service.embed("test")
    embedding.length shouldBe 384
  }

  test("embed should return non-zero vectors") {
    val embedding = service.embed("test")
    embedding.exists(_ != 0.0f) shouldBe true
  }

  test("embed should produce similar vectors for similar texts") {
    val e1         = service.embed("int main() { return 0; }")
    val e2         = service.embed("int main() { return 0; }")
    val similarity = TestFixtures.cosineSimilarity(e1, e2)
    similarity shouldBe 1.0 +- 0.001
  }

  test("embed should produce different vectors for different texts") {
    val e1         = service.embed("#include <stdio.h>\nint main() { printf(\"hello\"); return 0; }")
    val e2         = service.embed("use std::io; fn main() { println!(\"hello\"); }")
    val similarity = TestFixtures.cosineSimilarity(e1, e2)
    similarity should be < 0.95
  }

  test("similar code diffs should have higher similarity than unrelated code") {
    val eStudentA   = service.embed(TestFixtures.studentADiff)
    val ePlagiarism = service.embed(TestFixtures.plagiarismDIff)
    val eStudentC   = service.embed(TestFixtures.studentCDiff)
    val eRust       = service.embed(TestFixtures.unrelatedDiff)

    val simStudentAPlagiarism = TestFixtures.cosineSimilarity(eStudentA, ePlagiarism)
    val simStudentAStudentC   = TestFixtures.cosineSimilarity(eStudentA, eStudentC)
    val simStudentARust       = TestFixtures.cosineSimilarity(eStudentA, eRust)

    simStudentAPlagiarism should be > simStudentAStudentC
    simStudentAPlagiarism should be > simStudentARust
  }

  test("embedBatch should return correct number of embeddings") {
    val texts      = Seq("text one", "text two", "text three")
    val embeddings = service.embedBatch(texts)
    embeddings should have length 3
    embeddings.foreach(_.length shouldBe 384)
  }

  test("embedBatch should produce same results as individual embed calls") {
    val texts         = Seq("hello world", "test test test")
    val batchResults  = service.embedBatch(texts)
    val singleResults = texts.map(service.embed)

    batchResults.zip(singleResults).foreach { case (batch, single) =>
      val sim = TestFixtures.cosineSimilarity(batch, single)
      sim shouldBe 1.0 +- 0.001
    }
  }
