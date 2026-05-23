package plagiarism.core

case class FileDiff(
    path: String,
    content: String
)

case class Submission(
    studentName: String,
    repositoryPath: String,
    fileDiffs: List[FileDiff]
)

case class SimilarityResult(
    studentName: String,
    filePath: String,
    score: Float,
    storedDiff: String
)

case class FileCheckResult(
    queryFile: FileDiff,
    matches: List[SimilarityResult]
)

case class DiffLine(
    leftLineNum: Option[Int],
    leftContent: Option[String],
    rightLineNum: Option[Int],
    rightContent: Option[String],
    changeType: ChangeType
)

enum ChangeType:

  case Change, Delete, Insert, Equal
