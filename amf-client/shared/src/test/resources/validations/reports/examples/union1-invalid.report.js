Model: file://amf-client/shared/src/test/resources/validations/examples/union1-invalid.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message:  should be integer
 should be null
 should match some schema in anyOf

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/examples/union1-invalid.raml#/web-api/end-points/%2Ftest/get/request/parameter/date/union/schema/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/examples/union1-invalid.raml#/web-api/end-points/%2Ftest/get/request/parameter/date/union/schema/example/default-example
  Position: Some(LexicalInformation([(10,17)-(10,21)]))
  Location: file://amf-client/shared/src/test/resources/validations/examples/union1-invalid.raml
