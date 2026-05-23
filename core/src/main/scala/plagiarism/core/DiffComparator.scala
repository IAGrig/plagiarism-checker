package plagiarism.core

import com.github.difflib.DiffUtils
import com.github.difflib.patch.DeltaType

import scala.collection.mutable.ListBuffer as MutableList
import scala.jdk.CollectionConverters.*

object DiffComparator:

  def sideBySideDiff(original: String, updated: String): List[DiffLine] =
    val origLines = original.linesIterator.toList.asJava
    val updLines  = updated.linesIterator.toList.asJava
    val patch     = DiffUtils.diff(origLines, updLines)

    val result  = MutableList[DiffLine]()
    var origIdx = 0
    var updIdx  = 0

    val deltas = patch.getDeltas.asScala.toList.sortBy(_.getSource.getPosition)

    for delta <- deltas do
      val srcPos = delta.getSource.getPosition
      val tgtPos = delta.getTarget.getPosition

      // equal lines before delta
      while origIdx < srcPos && updIdx < tgtPos do
        val origLine = origLines.get(origIdx)
        result += DiffLine(
          Some(origIdx + 1),
          Some(origLine),
          Some(updIdx + 1),
          Some(origLine),
          ChangeType.Equal
        )
        origIdx += 1
        updIdx  += 1

      delta.getType match
        case DeltaType.DELETE =>
          for line <- delta.getSource.getLines.asScala do
            result += DiffLine(
              Some(origIdx + 1),
              Some(line),
              None,
              None,
              ChangeType.Delete
            )
            origIdx += 1

        case DeltaType.INSERT =>
          for line <- delta.getTarget.getLines.asScala do
            result += DiffLine(
              None,
              None,
              Some(updIdx + 1),
              Some(line),
              ChangeType.Insert
            )
            updIdx += 1

        case DeltaType.CHANGE =>
          val srcLines  = delta.getSource.getLines.asScala.toList
          val tgtLines  = delta.getTarget.getLines.asScala.toList
          val maxLength = math.max(srcLines.length, tgtLines.length)
          for i <- 0 until maxLength do
            val sl = if (i < srcLines.length) Some(srcLines(i)) else None
            val sr = if (i < tgtLines.length) Some(tgtLines(i)) else None
            result += DiffLine(
              sl.map(_ => origIdx + 1),
              sl,
              sr.map(_ => updIdx + 1),
              sr,
              ChangeType.Change
            )
            if (sl.isDefined) origIdx += 1
            if (sr.isDefined) updIdx  += 1

        case _ =>

    // remaining equal lines after the last delta
    while origIdx < origLines.size && updIdx < updLines.size do
      val origLine = origLines.get(origIdx)
      result += DiffLine(
        Some(origIdx + 1),
        Some(origLine),
        Some(updIdx + 1),
        Some(origLine),
        ChangeType.Equal
      )
      origIdx += 1
      updIdx  += 1

    result.toList
