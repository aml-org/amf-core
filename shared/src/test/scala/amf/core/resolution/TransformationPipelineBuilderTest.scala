package amf.core.resolution

import amf.core.client.common.transform.{PipelineId, PipelineName}
import amf.core.client.scala.errorhandling.{AMFErrorHandler, DefaultErrorHandler, UnhandledErrorHandler}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document.{BaseUnit, Document}
import amf.core.client.scala.transform.{
  TransformationPipeline,
  TransformationPipelineBuilder,
  TransformationPipelineRunner,
  TransformationStep
}
import amf.core.internal.remote.Amf
import amf.core.internal.validation.CoreValidations
import org.scalatest.{FunSuite, Matchers}

class TransformationPipelineBuilderTest extends FunSuite with Matchers {

  private case class AddToIdCustomStage(content: String) extends TransformationStep {
    override def transform(baseUnit: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
      baseUnit.withId(baseUnit.id + content)
    }
  }

  test("Create builder from empty pipeline and append stage") {
    val pipeline = TransformationPipelineBuilder.empty("defaultName").append(AddToIdCustomStage("modified")).build()

    val unit = Document().withId("")
    TransformationPipelineRunner(UnhandledErrorHandler).run(unit, pipeline)
    unit.id should be("modified")
  }

  test("Prepend and append to existing pipeline") {
    val createdPipeline: TransformationPipeline = new TransformationPipeline {
      override val name: String                   = "some-pipeline"
      override val steps: Seq[TransformationStep] = Seq(AddToIdCustomStage(" middle "))
    }
    val builder = TransformationPipelineBuilder
      .fromPipeline(createdPipeline)
      .prepend(AddToIdCustomStage("first"))
      .append(AddToIdCustomStage("last"))
    val unit     = Document().withId("")
    val pipeline = builder.build()
    TransformationPipelineRunner(UnhandledErrorHandler).run(unit, pipeline)
    unit.id should be("first middle last")
  }

  test("Pipeline builder name setter will override base pipeline name") {
    val basePipeline: TransformationPipeline = new TransformationPipeline {
      override val name: String                   = "originalName"
      override val steps: Seq[TransformationStep] = Nil
    }
    val newName = "otherName"
    val createdPipeline: TransformationPipeline =
      TransformationPipelineBuilder.fromPipeline(basePipeline).withName(newName).build()
    createdPipeline.name should be(newName)
  }

  test("Create builder from pipeline name and config") {
    val config = AMFGraphConfiguration.predefined()
    val builder =
      TransformationPipelineBuilder
        .fromPipeline(PipelineId.Default, config)
    val pipeline = builder.get.build()
    pipeline.steps should not be empty
  }

  test("Verify use of error handler in client stage") {
    val builder = TransformationPipelineBuilder.empty("defaultName")
    val pipeline = builder
      .append(new TransformationStep {
        override def transform(baseUnit: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
          errorHandler.violation(CoreValidations.ResolutionValidation, "node", "some error")
          baseUnit
        }
      })
      .build()

    val eh = DefaultErrorHandler()
    TransformationPipelineRunner(eh).run(Document(), pipeline)
    eh.getResults.size should be(1)
  }

}
