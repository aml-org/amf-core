Model: file://amf-client/shared/src/test/resources/validations/jsonschema/oneOf/api3.raml
Profile: RAML
Conforms? false
Number of results: 2

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: should match exactly one schema in oneOf
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/jsonschema/oneOf/api3.raml#/web-api/end-points/%2Fep3/get/200/application%2Fjson/any/schema/example/default-example
  Property: 
  Position: Some(LexicalInformation([(51,0)-(54,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/jsonschema/oneOf/api3.raml

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: bar should be integer
foo should be string
should match exactly one schema in oneOf

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/jsonschema/oneOf/api3.raml#/web-api/end-points/%2Fep4/get/200/application%2Fjson/any/schema/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/jsonschema/oneOf/api3.raml#/web-api/end-points/%2Fep4/get/200/application%2Fjson/any/schema/example/default-example
  Position: Some(LexicalInformation([(62,0)-(63,23)]))
  Location: file://amf-client/shared/src/test/resources/validations/jsonschema/oneOf/api3.raml
