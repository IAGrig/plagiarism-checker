package plagiarism.core

import java.io.File
import scala.sys.process.*
import scala.util.Try

object GitDiffExtractor:

  def getLastCommitHash(repoPath: String): Try[String] =
    Try {
      val result = Process(Seq("git", "log", "--format=%H", "-1"), new File(repoPath)).!!
      result.trim
    }

  def extractDiffs(
      studentRepoPath: String,
      teacherCommitHash: String,
      subDirectoryFilter: Option[String] = None
  ): Try[List[FileDiff]] =
    Try {
      val diffRange = s"$teacherCommitHash..HEAD"
      val cmd       = Seq("git", "diff", diffRange) ++ subDirectoryFilter.toSeq.flatMap(d => Seq("--", d))
      val rawDiff   = Process(cmd, new File(studentRepoPath)).!!
      parseDiffOutput(rawDiff)
    }

  def parseDiffOutput(rawDiff: String): List[FileDiff] =
    val diffPattern = """^diff --git a/.+ b/(.+)$""".r
    val lines       = rawDiff.linesIterator.toList

    val fileStarts = lines.zipWithIndex.collect { case (diffPattern(path), idx) =>
      (path, idx)
    }

    fileStarts.zipWithIndex.map { case ((path, startIdx), i) =>
      val endIdx  = if (i + 1 < fileStarts.length) fileStarts(i + 1)._2 else lines.length
      val content = lines.slice(startIdx, endIdx).mkString("\n")
      FileDiff(path, content)
    }

  def listStudentRepos(submissionsDir: String): List[(String, String)] =
    val dir = new File(submissionsDir)
    if (!dir.isDirectory) return List.empty
    dir
      .listFiles()
      .filter(_.isDirectory)
      .filterNot(_.getName.startsWith("."))
      .map { d =>
        val studentName = d.getName.split("_").head
        (studentName, d.getAbsolutePath)
      }
      .toList
      .sortBy(_._1)
