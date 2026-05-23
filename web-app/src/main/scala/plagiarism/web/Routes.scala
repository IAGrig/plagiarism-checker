package plagiarism.web

import plagiarism.vectordb.SimilaritySearchService

class Routes(searchService: SimilaritySearchService, userHost: String = "0.0.0.0", userPort: Int = 8080)
    extends cask.MainRoutes:

  override def host: String = userHost

  override def port: Int = userPort

  @cask.get("/")
  def index(): cask.Response[String] =
    cask.Response(
      HtmlViews.inputFormPage(),
      headers = Seq("Content-Type" -> "text/html; charset=utf-8")
    )

  @cask.postForm("/check")
  def check(code: String): cask.Response[String] =
    if (code.isBlank)
      cask.Response(
        HtmlViews.inputFormPage(Some("Paste a git diff")),
        headers = Seq("Content-Type" -> "text/html; charset=utf-8")
      )
    else
      val results = searchService.checkDiff(code)
      cask.Response(
        HtmlViews.resultsPage(results),
        headers = Seq("Content-Type" -> "text/html; charset=utf-8")
      )

  initialize()
