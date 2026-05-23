package plagiarism.web

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import plagiarism.core.FileCheckResult
import plagiarism.core.FileDiff
import plagiarism.core.SimilarityResult

class HtmlViewsSpec extends AnyFunSuite with Matchers:

  test("inputFormPage should contain a form with textarea") {
    val html = HtmlViews.inputFormPage()
    html should include("<form")
    html should include("textarea")
    html should include("name=\"code\"")
    html should include("action=\"/check\"")
    html should include("method=\"POST\"")
  }

  test("inputFormPage should display error message when provided") {
    val html = HtmlViews.inputFormPage(Some("Please paste a diff"))
    html should include("Please paste a diff")
    html should include("error")
  }

  test("inputFormPage should not display error section when no error") {
    val html = HtmlViews.inputFormPage(None)
    html should not include "class=\"error\""
  }

  test("resultsPage should display per-file sections with matches") {
    val fileResults = List(
      FileCheckResult(
        queryFile = FileDiff("main.c", "diff content"),
        matches = List(
          SimilarityResult("studentA", "main.c", 0.95f, "stored diff"),
          SimilarityResult("studentB", "main.c", 0.72f, "other diff")
        )
      ),
      FileCheckResult(
        queryFile = FileDiff("lib.c", "lib diff"),
        matches = List(
          SimilarityResult("studentC", "lib.c", 0.60f, "studentC diff")
        )
      )
    )
    val html = HtmlViews.resultsPage(fileResults)

    html should include("main.c")
    html should include("lib.c")

    html should include("studentA")
    html should include("studentB")
    html should include("studentC")

    html should include("95%")
    html should include("72%")
    html should include("60%")
  }

  test("resultsPage should show 'no results' message when empty") {
    val html = HtmlViews.resultsPage(List.empty)
    html should include("No similar submissions found")
  }

  test("resultsPage should use importance classes based on score") {
    val fileResults = List(
      FileCheckResult(
        queryFile = FileDiff("a.c", "diff"),
        matches = List(SimilarityResult("high_match", "a.c", 0.92f, "diff"))
      ),
      FileCheckResult(
        queryFile = FileDiff("b.c", "diff"),
        matches = List(SimilarityResult("medium_match", "b.c", 0.65f, "diff"))
      ),
      FileCheckResult(
        queryFile = FileDiff("c.c", "diff"),
        matches = List(SimilarityResult("low_match", "c.c", 0.30f, "diff"))
      )
    )
    val html = HtmlViews.resultsPage(fileResults)

    html should include("score-high")
    html should include("score-medium")
    html should include("score-low")
  }

  test("inputFormPage should be valid HTML with doctype") {
    val html = HtmlViews.inputFormPage()
    html should startWith("<!DOCTYPE html>")
    html should include("<html")
    html should include("</html>")
  }

  test("resultsPage should show match count per file") {
    val fileResults = List(
      FileCheckResult(
        queryFile = FileDiff("main.c", "diff"),
        matches = List(
          SimilarityResult("a", "main.c", 0.9f, "d"),
          SimilarityResult("b", "main.c", 0.8f, "d"),
          SimilarityResult("c", "main.c", 0.7f, "d")
        )
      )
    )
    val html = HtmlViews.resultsPage(fileResults)
    html should include("3 matches")
  }

  test("resultsPage should handle file with no matches") {
    val fileResults = List(
      FileCheckResult(
        queryFile = FileDiff("nomatchesfile.c", "diff"),
        matches = List.empty
      ),
      FileCheckResult(
        queryFile = FileDiff("main.c", "diff"),
        matches = List(
          SimilarityResult("a", "main.c", 0.99f, "d")
        )
      )
    )
    val html = HtmlViews.resultsPage(fileResults)
    html should include("nomatchesfile.c")
    html should include("no matches")
  }
