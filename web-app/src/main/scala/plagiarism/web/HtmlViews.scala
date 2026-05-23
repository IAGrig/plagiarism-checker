package plagiarism.web

import plagiarism.core.ChangeType
import plagiarism.core.DiffComparator
import plagiarism.core.FileCheckResult
import plagiarism.core.SimilarityResult
import scalatags.Text.all.*
import scalatags.Text.tags2.title as htmlTitle

object HtmlViews:

  def inputFormPage(error: Option[String] = None): String =
    "<!DOCTYPE html>" + html(
      head(
        htmlTitle("Plagiarism Checker"),
        meta(charset := "utf-8"),
        styleTag(cssStyles)
      ),
      body(
        div(cls := "container")(
          h1("Plagiarism Checker"),
          p(
            "Paste your ",
            code("git diff")
          ),
          error.map(e => div(cls := "error")(e)),
          formTag(method := "POST", action := "/check")(
            div(cls := "form-group")(
              label(`for` := "diff-input")("Git diff output:"),
              textarea(
                id          := "diff-input",
                name        := "code",
                rows        := 20,
                cols        := 100,
                placeholder := "diff output here"
              )
            ),
            button(`type` := "submit", cls := "btn")("Check")
          )
        )
      )
    ).render

  def resultsPage(results: List[FileCheckResult]): String =
    val totalMatches = results.map(_.matches.size).sum
    "<!DOCTYPE html>" + html(
      head(
        htmlTitle("Results - Plagiarism Checker"),
        meta(charset := "utf-8"),
        styleTag(cssStyles)
      ),
      body(
        div(cls := "container")(
          h1("Check Results"),
          p(cls := "summary")(
            s"Checked ${results.size} files, found $totalMatches matches total"
          ),
          if (results.isEmpty || results.forall(_.matches.isEmpty))
            div(cls := "no-results")(
              p("No similar submissions found in the database")
            )
          else
            div(cls := "results")(
              results.map(fileSection)
            ),
          hr,
          a(href := "/")("Back")
        )
      )
    ).render

  private def fileSection(result: FileCheckResult): Frag =
    val bestScore: Long   = result.matches.headOption.map(r => (r.score * 100).round.toLong).getOrElse(0L)
    val significanceLevel =
      if (bestScore >= 80) "high"
      else if (bestScore >= 50) "medium"
      else "low"

    div(cls := s"file-section $significanceLevel")(
      tag("details")(
        tag("summary")(cls := "file-header")(
          span(cls := "file-path-title")(result.queryFile.path),
          span(cls := s"score score-$significanceLevel")(
            if (result.matches.nonEmpty) s"best match: $bestScore%"
            else "no matches"
          ),
          span(cls := "match-count")(s"${result.matches.size} match(es)")
        ),
        if (result.matches.isEmpty) p(cls := "no-matches-note")("No similar files found")
        else
          div(cls := "match-list")(
            result.matches.zipWithIndex.map { case (r, idx) =>
              matchCard(r, idx, result.queryFile.content)
            }
          )
      )
    )

  private def matchCard(result: SimilarityResult, index: Int, queryFileContent: String): Frag =
    val percentage    = (result.score * 100).round
    val severityClass =
      if (percentage >= 80) "high"
      else if (percentage >= 50) "medium"
      else "low"

    div(cls := s"result-card $severityClass")(
      div(cls := "result-header")(
        span(cls := "rank")(s"#${index + 1}"),
        span(cls := "student-name")(result.studentName),
        span(cls := "file-path")(result.filePath),
        span(cls := s"score score-$severityClass")(s"$percentage% similar")
      ),
      tag("details")(
        tag("summary")("Show side-by-side"),
        div(cls := "diff-container")(
          sideBySideDiff(result.storedDiff, queryFileContent, result.studentName)
        )
      )
    )

  private def sideBySideDiff(storedDiff: String, queryDiff: String, studentName: String): Frag =
    val diffLines = DiffComparator.sideBySideDiff(storedDiff, queryDiff)
    table(cls := "diff-table")(
      thead(
        tr(
          th(colspan := 2)(s"Database ($studentName)"),
          th(colspan := 2)("You")
        )
      ),
      tbody(
        diffLines.map { dl =>
          val rowClass = dl.changeType match
            case ChangeType.Equal  => "diff-equal"
            case ChangeType.Insert => "diff-insert"
            case ChangeType.Delete => "diff-delete"
            case ChangeType.Change => "diff-change"
          tr(cls := rowClass)(
            td(cls := "line-num")(dl.leftLineNum.map(_.toString).getOrElse("")),
            td(cls := "line-content")(dl.leftContent.getOrElse("")),
            td(cls := "line-num")(dl.rightLineNum.map(_.toString).getOrElse("")),
            td(cls := "line-content")(dl.rightContent.getOrElse(""))
          )
        }
      )
    )

  private val cssStyles: String =
    """
      |* { box-sizing: border-box; margin: 0; padding: 0; }
      |body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, monospace;
      |       background: #f5f5f5; color: #333; line-height: 1.6; }
      |.container { max-width: 1200px; margin: 0 auto; padding: 2rem; }
      |h1 { margin-bottom: 1rem; color: #1a1a2e; }
      |.form-group { margin-bottom: 1rem; }
      |label { display: block; margin-bottom: 0.5rem; font-weight: bold; }
      |textarea { width: 100%; font-family: 'Courier New', monospace; font-size: 13px;
      |           padding: 0.75rem; border: 1px solid #ccc; border-radius: 4px;
      |           background: #fff; }
      |.btn { background: #1a1a2e; color: white; border: none; padding: 0.75rem 2rem;
      |        font-size: 1rem; border-radius: 4px; cursor: pointer; margin-top: 0.5rem; }
      |.btn:hover { background: #16213e; }
      |.error { background: #fee; border: 1px solid #fcc; color: #c00;
      |         padding: 0.75rem; border-radius: 4px; margin-bottom: 1rem; }
      |.summary { margin-bottom: 1.5rem; font-size: 1.1rem; }
      |.file-section { background: #fff; border: 1px solid #ddd; border-radius: 8px;
      |                margin-bottom: 1.5rem; overflow: hidden; }
      |.file-section.high { border-left: 4px solid #e74c3c; }
      |.file-section.medium { border-left: 4px solid #f39c12; }
      |.file-section.low { border-left: 4px solid #27ae60; }
      |.file-header { display: flex; align-items: center; gap: 1rem;
      |               padding: 1rem; font-size: 1rem; cursor: pointer; }
      |.file-path-title { font-weight: bold; font-family: monospace; font-size: 1.05rem; }
      |.match-count { color: #888; font-size: 0.85rem; }
      |.match-list { padding: 0 1rem 1rem; }
      |.no-matches-note { padding: 1rem; color: #888; }
      |.result-card { background: #fafafa; border: 1px solid #eee; border-radius: 6px;
      |               margin-bottom: 0.75rem; overflow: hidden; }
      |.result-card.high { border-left: 3px solid #e74c3c; }
      |.result-card.medium { border-left: 3px solid #f39c12; }
      |.result-card.low { border-left: 3px solid #27ae60; }
      |.result-header { display: flex; align-items: center; gap: 1rem;
      |                 padding: 0.75rem 1rem; }
      |.rank { font-weight: bold; font-size: 1.1rem; color: #666; }
      |.student-name { font-weight: bold; font-size: 1rem; }
      |.file-path { color: #666; font-family: monospace; font-size: 0.85rem; }
      |.score { margin-left: auto; padding: 0.25rem 0.75rem; border-radius: 12px;
      |         font-weight: bold; font-size: 0.85rem; }
      |.score-high { background: #fde; color: #c0392b; }
      |.score-medium { background: #fef3cd; color: #856404; }
      |.score-low { background: #d4edda; color: #155724; }
      |details { padding: 0 1rem 1rem; }
      |summary { cursor: pointer; padding: 0.5rem 0; color: #555; font-size: 0.9rem; }
      |.diff-container { overflow-x: auto; margin-top: 0.5rem; }
      |.diff-table { width: 100%; border-collapse: collapse; font-family: monospace;
      |              font-size: 12px; }
      |.diff-table th { background: #eee; padding: 0.5rem; text-align: left;
      |                 border-bottom: 2px solid #ccc; }
      |.diff-table td { padding: 1px 0.5rem; border-bottom: 1px solid #eee;
      |                 white-space: pre-wrap; vertical-align: top; }
      |.line-num { width: 40px; color: #999; text-align: right;
      |            user-select: none; border-right: 1px solid #ddd; }
      |.line-content { min-width: 200px; }
      |.diff-equal { }
      |.diff-insert td.line-content:last-child { background: #e6ffec; }
      |.diff-delete td.line-content:nth-child(2) { background: #ffebe9; }
      |.diff-change td.line-content:nth-child(2) { background: #ffebe9; }
      |.diff-change td.line-content:last-child { background: #e6ffec; }
      |.no-results { background: #fff; padding: 2rem; border-radius: 8px;
      |              text-align: center; color: #666; }
      |hr { border: none; border-top: 1px solid #ddd; margin: 2rem 0 1rem; }
      |a { color: #1a1a2e; }
      |""".stripMargin

  private def formTag = tag("form")

  private def styleTag = tag("style")
