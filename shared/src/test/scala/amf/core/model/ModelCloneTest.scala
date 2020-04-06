package amf.core.model
import amf.core.annotations.{ErrorDeclaration, SourceVendor}
import amf.core.metamodel.Obj
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.document.Document
import amf.core.model.domain.{ArrayNode, LinkNode, ObjectNode, ScalarNode}
import amf.core.parser.{Annotations, Fields}
import amf.core.remote.Raml10
import amf.core.render.ElementsFixture
import amf.core.vocabulary.Namespace.XsdTypes
import org.scalatest.{FunSuite, Matchers}

import scala.collection.mutable

class ModelCloneTest extends FunSuite with ElementsFixture with Matchers{

  test("Test clone encoded at Document"){
    val cloned :Document = document.cloneUnit().asInstanceOf[Document]
    cloned.encodes.asInstanceOf[ScalarNode].withValue("ClonedValue")

    document.encodes.asInstanceOf[ScalarNode].value.value() should be("myValue")
    cloned.encodes.asInstanceOf[ScalarNode].value.value() should be("ClonedValue")
  }

  test("Test clone object node"){
    val cloned: ObjectNode = objectNode.cloneElement(mutable.Map.empty).asInstanceOf[ObjectNode]

    cloned.addProperty("newProp",ScalarNode("newValue",Some(DataType.String)))

    objectNode.allProperties().size should be(1)
    cloned.allProperties().size should be(2)

    cloned.allPropertiesWithName()("myProp1").asInstanceOf[ArrayNode].addMember(ScalarNode("new2", Some(DataType.String)))

    arrayNode.members.length should be(1)
    objectNode.allPropertiesWithName()("myProp1").asInstanceOf[ArrayNode].members.length should be(1)
    cloned.allPropertiesWithName()("myProp1").asInstanceOf[ArrayNode].members.length should be(2)
  }


  test("Test clone recursive object"){
    val cloned = recursiveObj.cloneElement(mutable.Map.empty).asInstanceOf[ObjectNode]

    cloned.allProperties().head.asInstanceOf[ArrayNode].members.head.asInstanceOf[ObjectNode].allProperties().head should be(cloned)
    cloned.allProperties().head.asInstanceOf[ArrayNode].members.head.asInstanceOf[ObjectNode].id should be(recursiveObj.allProperties().head.asInstanceOf[ArrayNode].members.head.asInstanceOf[ObjectNode].id)
    succeed
  }

  test("Test annotations at object"){
    objectNode.annotations += SourceVendor(Raml10)
    val cloned = objectNode.cloneElement(mutable.Map.empty).asInstanceOf[ObjectNode]

    cloned.annotations.contains(classOf[SourceVendor]) should be(true)
  }

  test("Test clone document and reference (parserRun)"){
    documentWithRef.withRunNumber(1)
    module.withRunNumber(1)
    val cloned :Document = documentWithRef.cloneUnit().asInstanceOf[Document]
    cloned.parserRun.get should be(1)

    cloned.references.head.parserRun.get should be(1)
  }

  test("Test clone document with duplicated ids"){
    val localNode = ObjectNode().withId(objectNode.id).addProperty("localProp",ScalarNode().withId("amf://localId").withDataType(XsdTypes.xsdString.iri()).withValue("aValue"))
    val doc = Document().withId("amf://localDoc").withDeclares(Seq(objectNode, localNode))
    val cloned :Document = doc.cloneUnit().asInstanceOf[Document]
    val obj = cloned.declares.head
    val local = cloned.declares.last

    obj.id should be(local.id)

    obj.asInstanceOf[ObjectNode].allPropertiesWithName().keySet.head should be("myProp1")
    local.asInstanceOf[ObjectNode].allPropertiesWithName().keySet.head should be("localProp")
  }

  test("Test clone link node with internal linked domain element "){
    val scalarNode = ScalarNode("linkValue", Some(XsdTypes.xsdString.iri())).withId("amf://linkNode1")
    val linkNode = LinkNode("link", "linkValue").withId("amf://linkNode2").withLinkedDomainElement( scalarNode)

    val cloned = linkNode.cloneElement(mutable.Map.empty)
    cloned.linkedDomainElement.get.id should be(scalarNode.id)

  }

  test("Test clone error declaration"){
    trait Error extends ErrorDeclaration {


      override def meta: Obj = DomainElementModel

      /** Set of fields composing object. */
      override val fields: Fields = Fields()

      /** Value , path + field value that is used to compose the id when the object its adopted */
      override def componentId: String = "erro1"

      /** Set of annotations for element. */
      override val annotations: Annotations = Annotations()
    }

    case class Error1() extends Error {
      override val namespace: String = "http://errorDeclaration#error1"

      override def newErrorInstance: ErrorDeclaration = Error1()
    }

    case class Error2() extends Error {
      override val namespace: String = "http://errorDeclaration#error2"

      override def newErrorInstance: ErrorDeclaration = Error2()
    }

    val error1 = Error1()
    val error2 = Error2()

    val document1 = Document().withId("amf://id1").withDeclares(Seq(error1, error1))
    val cloned = document1.cloneUnit()
    val declares = cloned.asInstanceOf[Document].declares
    val error1Cloned = declares.head
    error1Cloned.isInstanceOf[Error1] should be(true)
    (error1Cloned == error1) should be(false)
    error2.isInstanceOf[Error2] should be(true)


  }
}
