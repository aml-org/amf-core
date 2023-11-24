package amf.core.resolution

import amf.core.client.platform.AMFGraphConfiguration
import amf.core.client.platform.model.document.Document
import amf.core.client.scala.model.domain.extensions.CustomDomainProperty
import amf.core.internal.convert.{BaseUnitConverter, NativeOps}
import amf.core.io.FileAssertionTest
import amf.core.render.ElementsFixture
import org.scalatest.matchers.should.Matchers

trait BasicResolutionTest
    extends FileAssertionTest
    with NativeOps
    with BaseUnitConverter
    with Matchers
    with ElementsFixture {

  test("test basic link resolution") {

    val domainProperty = CustomDomainProperty().withName("myProperty").withId("amf://id6")
    document.withEncodes(domainProperty.link("myLink"))
    document.encodes.asInstanceOf[CustomDomainProperty].linkTarget.isEmpty shouldBe false
    val result = AMFGraphConfiguration.predefined().baseUnitClient().transform(BaseUnitMatcher.asClient(document))
    result.baseUnit
      .asInstanceOf[Document]
      .encodes
      .asInstanceOf[amf.core.client.platform.model.domain.CustomDomainProperty]
      .linkTarget
      .asOption
      .isEmpty shouldBe true
  }
}
