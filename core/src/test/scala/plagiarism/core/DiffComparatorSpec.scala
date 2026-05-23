package plagiarism.core

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class DiffComparatorSpec extends AnyFunSuite with Matchers:

  test("sideBySideDiff of identical texts should produce only Equal lines") {
    val text =
      """line1
        |line2
        |line3""".stripMargin
    val result = DiffComparator.sideBySideDiff(text, text)

    result should have length 3
    result.foreach { dl =>
      dl.changeType shouldBe ChangeType.Equal
      dl.leftContent shouldBe dl.rightContent
    }
  }

  test("sideBySideDiff should detect inserted lines") {
    val original =
      """line1
        |line3""".stripMargin
    val updated =
      """line1
        |line2
        |line3""".stripMargin
    val result = DiffComparator.sideBySideDiff(original, updated)

    result should have length 3
    result.count(_.changeType == ChangeType.Equal) shouldBe 2
    result.count(_.changeType == ChangeType.Insert) shouldBe 1

    val inserted = result.find(_.changeType == ChangeType.Insert).get
    inserted.rightContent shouldBe Some("line2")
    inserted.leftContent shouldBe None
  }

  test("sideBySideDiff should detect deleted lines") {
    val original =
      """line1
        |line2
        |line3""".stripMargin
    val updated =
      """line1
        |line3""".stripMargin
    val result = DiffComparator.sideBySideDiff(original, updated)

    result should have length 3
    val deleted = result.find(_.changeType == ChangeType.Delete).get
    deleted.leftContent shouldBe Some("line2")
    deleted.rightContent shouldBe None
  }

  test("sideBySideDiff should detect changed lines") {
    val original =
      """line1
        |old
        |line3""".stripMargin
    val updated =
      """line1
        |new
        |line3""".stripMargin
    val result = DiffComparator.sideBySideDiff(original, updated)

    result should have length 3
    val changed = result.find(_.changeType == ChangeType.Change).get
    changed.leftContent shouldBe Some("old")
    changed.rightContent shouldBe Some("new")
  }

  test("sideBySideDiff with empty original should show all inserts") {
    val result = DiffComparator.sideBySideDiff("", "new1\nnew2")
    result.foreach(_.changeType shouldBe ChangeType.Insert)
    result.flatMap(_.rightContent) shouldBe List("new1", "new2")
  }

  test("sideBySideDiff with empty updated should show all deletes") {
    val result = DiffComparator.sideBySideDiff("old1\nold2", "")
    result.foreach(_.changeType shouldBe ChangeType.Delete)
    result.flatMap(_.leftContent) shouldBe List("old1", "old2")
  }

  test("sideBySideDiff should handle multiline changes correctly") {
    val original =
      """a
        |b
        |c
        |d
        |e""".stripMargin
    val updated =
      """a
        |X
        |Y
        |Z
        |e""".stripMargin
    val result = DiffComparator.sideBySideDiff(original, updated)

    val equal   = result.filter(_.changeType == ChangeType.Equal)
    val changed = result.filter(_.changeType == ChangeType.Change)

    equal should have length 2
    changed should have length 3
  }
