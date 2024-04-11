package amf.core.internal.utils

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable

class GraphCycleDetectorTest extends AnyFunSuite with Matchers {

  test("Simple linear graph should not have cycles") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "B" -> Set("C"),
      "C" -> Set("D")
    )

    GraphCycleDetector.hasCycles(graph, "A") shouldBe false
  }

  test("Simple linear graph should not have cycles starting in other node") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "B" -> Set("C"),
      "C" -> Set("D")
    )

    GraphCycleDetector.hasCycles(graph, "B") shouldBe false
  }

  test("Graph with simple cycle should be detected from A") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "B" -> Set("C"),
      "C" -> Set("A")
    )

    GraphCycleDetector.hasCycles(graph, "A") shouldBe true
  }

  test("Graph with simple cycle should be detected from B") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "B" -> Set("C"),
      "C" -> Set("A")
    )

    GraphCycleDetector.hasCycles(graph, "B") shouldBe true
  }

  test("Graph with branches should not have cycles") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "A" -> Set("C"),
      "B" -> Set("D"),
      "B" -> Set("E"),
      "C" -> Set("F"),
      "C" -> Set("G")
    )

    GraphCycleDetector.hasCycles(graph, "A") shouldBe false
  }

  test("Graph with shared parents should not have cycles") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "A" -> Set("C"),
      "B" -> Set("D"),
      "B" -> Set("E"),
      "C" -> Set("D"),
      "C" -> Set("E"),
      "D" -> Set("F"),
      "E" -> Set("F"),
      "F" -> Set("G")
    )

    GraphCycleDetector.hasCycles(graph, "A") shouldBe false
  }

  test("Graph with shared parents and cycles should be detected") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "A" -> Set("C"),
      "B" -> Set("D"),
      "B" -> Set("E"),
      "C" -> Set("D"),
      "C" -> Set("E"),
      "D" -> Set("F"),
      "E" -> Set("F"),
      "F" -> Set("A")
    )

    GraphCycleDetector.hasCycles(graph, "A") shouldBe true
  }

  test("Graph with shared parents and cycles should be detected starting from other node") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "A" -> Set("C"),
      "B" -> Set("D"),
      "B" -> Set("E"),
      "C" -> Set("D"),
      "C" -> Set("E"),
      "D" -> Set("F"),
      "E" -> Set("F"),
      "F" -> Set("A")
    )

    GraphCycleDetector.hasCycles(graph, "B") shouldBe true
  }

  test("Graph with shared parents and cycles should not be detected is starting from node without cycles") {
    val graph = mutable.Map(
      "A" -> Set("B"),
      "A" -> Set("C"),
      "B" -> Set("D"),
      "B" -> Set("E"),
      "B" -> Set("G"),
      "G" -> Set("H"),
      "G" -> Set("I"),
      "C" -> Set("D"),
      "C" -> Set("E"),
      "D" -> Set("F"),
      "E" -> Set("F"),
      "F" -> Set("A")
    )

    GraphCycleDetector.hasCycles(graph, "G") shouldBe false
  }

}
