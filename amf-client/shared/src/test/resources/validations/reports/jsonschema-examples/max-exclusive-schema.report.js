Model: file://amf-client/shared/src/test/resources/validations/jsonschema/max-exclusive-schema.raml
Profile: RAML 1.008
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message:  should be < 180
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/jsonschema/max-exclusive-schema.raml#/declarations/schemas/invalidExample/property/prop1/scalar/prop1/example/default-example
  Property: 
  Position: Some(LexicalInformation([(20,36)-(20,39)]))
  Location: file://amf-client/shared/src/test/resources/validations/jsonschema/max-exclusive-schema.raml
