Model: file://amf-cli/shared/src/test/resources/validations/examples/named-examples-seq-invalid/api.raml
Profile: RAML 1.0
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: [1].age should be integer
  Level: Violation
  Target: file://amf-cli/shared/src/test/resources/validations/examples/named-examples-seq-invalid/api.raml#/declarations/types/array/People/example/default-example
  Property: file://amf-cli/shared/src/test/resources/validations/examples/named-examples-seq-invalid/api.raml#/declarations/types/array/People/example/default-example
  Position: Some(LexicalInformation([(2,0)-(5,11)]))
  Location: file://amf-cli/shared/src/test/resources/validations/examples/named-examples-seq-invalid/example.raml
