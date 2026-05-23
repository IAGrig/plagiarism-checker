package plagiarism.vectordb

import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import plagiarism.testutils.TestFixtures

class EmbeddingServiceIT extends AnyFunSuite with Matchers with BeforeAndAfterAll:

  private var service: EmbeddingService = _

  override def beforeAll(): Unit =
    super.beforeAll()
    service = EmbeddingService.create()

  override def afterAll(): Unit =
    if (service != null) service.close()
    super.afterAll()

  test("embedding real code diff should produce valid vectors") {
    val embedding = service.embed(TestFixtures.studentADiff)
    embedding.length shouldBe 384
    embedding.exists(_ != 0.0f) shouldBe true

    embedding.foreach { v =>
      v.isNaN shouldBe false
      v.isInfinite shouldBe false
    }
  }

  test("similar code diffs should have high similarity") {
    val eStudentA   = service.embed(TestFixtures.studentADiff)
    val ePlagiarims = service.embed(TestFixtures.plagiarismDIff)
    val sim         = TestFixtures.cosineSimilarity(eStudentA, ePlagiarims)

    sim should be > 0.9
  }

  test("different code should have lower similarity than plagiarism") {
    val eStudentA   = service.embed(TestFixtures.studentADiff)
    val ePlagiarims = service.embed(TestFixtures.plagiarismDIff)
    val eStudentC   = service.embed(TestFixtures.studentCDiff)

    val simCopyPaste = TestFixtures.cosineSimilarity(eStudentA, ePlagiarims)
    val simDifferent = TestFixtures.cosineSimilarity(eStudentA, eStudentC)

    simCopyPaste should be > simDifferent
  }

  test("unrelated code should have low similarity") {
    val eStudentA = service.embed(TestFixtures.studentADiff)
    val eRust     = service.embed(TestFixtures.unrelatedDiff)

    val simUnrelated = TestFixtures.cosineSimilarity(eStudentA, eRust)

    simUnrelated should be < 0.85
  }

  test("embedding a long code diff should not throw") {
    val longDiff = TestFixtures.studentADiff * 100 + "\n" * 100 + TestFixtures.studentCDiff * 100
    noException should be thrownBy {
      val embedding = service.embed(longDiff)
      embedding.length shouldBe 384
    }
  }

  test("embedding empty string should produce a valid vector") {
    val embedding = service.embed("")
    embedding.length shouldBe 384
  }

  test("batch embedding should produce consistent results with single embedding") {
    val texts = Seq(
      TestFixtures.studentADiff,
      TestFixtures.studentCDiff,
      TestFixtures.unrelatedDiff
    )

    val batchEmbeddings  = service.embedBatch(texts)
    val singleEmbeddings = texts.map(service.embed)

    batchEmbeddings.zip(singleEmbeddings).foreach { case (batch, single) =>
      val sim = TestFixtures.cosineSimilarity(batch, single)
      sim shouldBe 1.0 +- 0.01
    }
  }
