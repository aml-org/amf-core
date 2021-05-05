package amf.core.resolution

import amf.client.parse.{DefaultErrorHandler, DefaultParserErrorHandler}
import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.resolution.{PipelineName, TransformationPipelineBuilder}
import amf.core.errorhandling.{ErrorHandler, UnhandledErrorHandler}
import amf.core.model.document.{BaseUnit, Document}
import amf.core.remote.Amf
import amf.core.resolution.pipelines.TransformationPipeline
import amf.core.resolution.stages.TransformationStep
import amf.plugins.features.validation.CoreValidations
import org.scalatest.{FunSuite, Matchers}

class TransformationPipelineBuilderTest extends FunSuite with Matchers {

  private case class AddToIdCustomStage(content: String) extends TransformationStep {
    override def apply[T <: BaseUnit](baseUnit: T, errorHandler: ErrorHandler): T = {
      baseUnit.withId(baseUnit.id + content)
    }
  }

  test("Create builder from empty pipeline and append stage") {
    val pipeline = TransformationPipelineBuilder.empty().append(AddToIdCustomStage("modified")).build()

    val unit = Document().withId("")
    pipeline.transform(unit, UnhandledErrorHandler)
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
    val unit = Document().withId("")
    builder.build().transform(unit, UnhandledErrorHandler)
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
      TransformationPipelineBuilder.fromPipeline(PipelineName.from(Amf.name, TransformationPipeline.DEFAULT_PIPELINE),
                                                 config)
    val pipeline = builder.get.build()
    pipeline.steps should not be empty
  }

  test("Verify use of error handler in client stage") {
    val builder = TransformationPipelineBuilder.empty()
    val pipeline = builder
      .append(new TransformationStep {
        override def apply[T <: BaseUnit](baseUnit: T, errorHandler: ErrorHandler): T = {
          errorHandler.violation(CoreValidations.ResolutionValidation, "node", "some error")
          baseUnit
        }
      })
      .build()

    val eh = DefaultErrorHandler()
    pipeline.transform(Document(), eh)
    eh.getErrors.size should be(1)
  }

}
