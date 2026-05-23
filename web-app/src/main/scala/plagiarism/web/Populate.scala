package plagiarism.web

import org.slf4j.LoggerFactory
import plagiarism.core.FileDiff
import plagiarism.core.GitDiffExtractor
import plagiarism.vectordb.EmbeddingService
import plagiarism.vectordb.QdrantRepository
import plagiarism.vectordb.SimilaritySearchService

import scala.util.Failure
import scala.util.Success

// sbt "web-app/runMain plagiarism.web.populate <studentsReposDir> <teacherRepoDir> [qdrantHost] [qdrantPort]"
@main def populate(
    studentSubmissionsDir: String,
    teacherRepoDir: String,
    qdrantHost: String = "localhost",
    qdrantGrpcPort: Int = 6334
): Unit =
  val logger         = LoggerFactory.getLogger("Populate")
  val collectionName = "submissions"

  val teacherHash = GitDiffExtractor.getLastCommitHash(teacherRepoDir) match
    case Success(hash) =>
      logger.info(s"Teacher's last commit: $hash")
      hash
    case Failure(ex) =>
      logger.error(s"Failed to get teacher commit hash: ${ex.getMessage}")
      sys.exit(1)

  logger.info("Loading embedding model")
  val embeddingService = EmbeddingService.create()
  val qdrantRepo       = new QdrantRepository(qdrantHost, qdrantGrpcPort)
  val searchService    = new SimilaritySearchService(embeddingService, qdrantRepo, collectionName)

  qdrantRepo.createCollectionIfNotExists(collectionName, embeddingService.dimension)

  val studentRepos = GitDiffExtractor.listStudentRepos(studentSubmissionsDir)
  logger.info(s"Found ${studentRepos.size} student repositories")

  var totalVectors = 0L
  var idCounter    = 0L
  var successCount = 0
  var failCount    = 0

  for (studentName, repoPath) <- studentRepos do
    GitDiffExtractor.extractDiffs(repoPath, teacherHash, Some("solution/")) match
      case Success(fileDiffs) if fileDiffs.nonEmpty =>
        val count = searchService.upsertSubmission(studentName, fileDiffs, idCounter)
        idCounter    += count
        totalVectors += count
        successCount += 1
        logger.info(s"  [$successCount/${studentRepos.size}] $studentName: $count file(s) ingested")

      case Success(_) =>
        logger.warn(s"  $studentName: no diffs found (skipping)")
        successCount += 1

      case Failure(ex) =>
        logger.warn(s"  $studentName: failed to extract diffs - ${ex.getMessage}")
        failCount += 1

  logger.info(s"Total students succeed: $successCount")
  logger.info(s"Total students failed: $failCount")
  logger.info(s"Total vectors in database: $totalVectors")

  embeddingService.close()
  qdrantRepo.close()
