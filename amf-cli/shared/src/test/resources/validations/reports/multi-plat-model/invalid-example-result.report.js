Model: file://amf-cli/shared/src/test/resources/validations/raml/nested-json-schema-external-fragment/api.raml
Profile: RAML 0.8
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: A should be integer
should have required property 'B'

  Level: Violation
  Target: file://amf-cli/shared/src/test/resources/validations/raml/nested-json-schema-external-fragment/api.raml#/web-api/end-points/%2FsomeEndpoint/get/200/application%2Fjson/application%2Fjson/example/default-example
  Property: file://amf-cli/shared/src/test/resources/validations/raml/nested-json-schema-external-fragment/api.raml#/web-api/end-points/%2FsomeEndpoint/get/200/application%2Fjson/application%2Fjson/example/default-example
  Position: Some(LexicalInformation([(13,24)-(15,25)]))
  Location: file://amf-cli/shared/src/test/resources/validations/raml/nested-json-schema-external-fragment/api.raml