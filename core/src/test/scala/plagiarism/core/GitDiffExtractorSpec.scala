package plagiarism.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.io.File
import java.nio.file.Files

class GitDiffExtractorSpec extends AnyFunSuite with Matchers:

  test("parseDiffOutput should parse a single file diff") {
    val rawDiff =
      """diff --git a/solution/src/main.c b/solution/src/main.c
        |--- a/solution/src/main.c
        |+++ b/solution/src/main.c
        |@@ -1,3 +1,5 @@
        |+#include <stdio.h>
        | int main() {
        |-    return 0;
        |+    printf("hello\n");
        |+    return 0;
        | }""".stripMargin

    val result = GitDiffExtractor.parseDiffOutput(rawDiff)

    result should have length 1
    result.head.path shouldBe "solution/src/main.c"
    result.head.content should include("main.c")
    result.head.content should include("+#include <stdio.h>")
  }

  test("parseDiffOutput should parse multiple file diffs") {
    val rawDiff =
      """diff --git a/src/a.c b/src/a.c
        |--- a/src/a.c
        |+++ b/src/a.c
        |@@ -1,3 +1,3 @@
        |-old line
        |+new line
        |diff --git a/src/b.c b/src/b.c
        |--- /dev/null
        |+++ b/src/b.c
        |@@ -0,0 +1,5 @@
        |+int b() {
        |+    return 42;
        |+}
        |diff --git a/include/def.h b/include/def.h
        |--- /dev/null
        |+++ b/include/def.h
        |@@ -0,0 +1,3 @@
        |+#ifndef DEF_H
        |+#define DEF_H
        |+#endif""".stripMargin

    val result = GitDiffExtractor.parseDiffOutput(rawDiff)

    result should have length 3
    result.map(_.path) shouldBe List("src/a.c", "src/b.c", "include/def.h")
    result(0).content should include("-old line")
    result(1).content should include("+int b()")
    result(2).content should include("DEF_H")
  }

  test("parseDiffOutput should handle empty input") {
    GitDiffExtractor.parseDiffOutput("") shouldBe List.empty
    GitDiffExtractor.parseDiffOutput("  \n  ") shouldBe List.empty
  }

  test("parseDiffOutput should handle binary file diffs gracefully") {
    val rawDiff =
      """diff --git a/image.bmp b/image.bmp
        |Binary files /dev/null and b/image.bmp differ
        |diff --git a/src/main.c b/src/main.c
        |--- a/src/main.c
        |+++ b/src/main.c
        |@@ -1 +1,2 @@
        |-return 0;
        |+printf("test");
        |+return 0;""".stripMargin

    val result = GitDiffExtractor.parseDiffOutput(rawDiff)

    result should have length 2
    result(0).path shouldBe "image.bmp"
    result(1).path shouldBe "src/main.c"
  }

  test("listStudentRepos should return empty for non-existent directory") {
    GitDiffExtractor.listStudentRepos("/nonexistent/path") shouldBe List.empty
  }

  test("listStudentRepos should extract student names from directory names") {
    val tmpDir = Files.createTempDirectory("test-repos")
    try {
      Files.createDirectory(tmpDir.resolve("student-a_assignment-3_12345"))
      Files.createDirectory(tmpDir.resolve("student-b_assignment-3_12346"))
      Files.createDirectory(tmpDir.resolve(".hidden"))

      val result = GitDiffExtractor.listStudentRepos(tmpDir.toString)

      result should have length 2
      result.map(_._1) shouldBe List("student-a", "student-b")
      result.foreach { case (_, path) =>
        new File(path).isDirectory shouldBe true
      }
    } finally {
      tmpDir.toFile.listFiles().foreach(_.delete())
      tmpDir.toFile.delete()
    }
  }
