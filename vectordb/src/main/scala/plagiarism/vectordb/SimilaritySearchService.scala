package plagiarism.vectordb

import org.slf4j.LoggerFactory
import plagiarism.core.FileCheckResult
import plagiarism.core.FileDiff
import plagiarism.core.GitDiffExtractor
import plagiarism.core.SimilarityResult

class SimilaritySearchService(
    embeddingService: EmbeddingService,
    qdrantRepository: QdrantRepository,
    collectionName: String
):

  private val logger = LoggerFactory.getLogger(getClass)

  def checkDiff(rawDiff: String, topK: Int = 5): List[FileCheckResult] =
    val fileDiffs = GitDiffExtractor.parseDiffOutput(rawDiff)
    if (fileDiffs.isEmpty) return List.empty

    fileDiffs.map { diff =>
      val matches = searchSimilar(diff.content, topK)
      FileCheckResult(queryFile = diff, matches = matches)
    }

  def searchSimilar(diffText: String, topK: Int = 5): List[SimilarityResult] =
    val embedding = embeddingService.embed(diffText)
    val results   = qdrantRepository.search(collectionName, embedding, topK)

    results.map { res =>
      SimilarityResult(
        studentName = res.payload.getOrElse("student", "unknown"),
        filePath = res.payload.getOrElse("file_path", "unknown"),
        score = res.score,
        storedDiff = res.payload.getOrElse("diff_text", "")
      )
    }

  def upsertSubmission(
      studentName: String,
      fileDiffs: List[FileDiff],
      startId: Long
  ): Int =
    val points = fileDiffs.zipWithIndex.map { case (diff, idx) =>
      val embedding = embeddingService.embed(diff.content)
      val payload   = Map(
        "student"   -> studentName,
        "file_path" -> diff.path,
        "diff_text" -> diff.content
      )
      (startId + idx, embedding, payload)
    }
    if (points.nonEmpty) qdrantRepository.upsertBatch(collectionName, points)
    points.size
