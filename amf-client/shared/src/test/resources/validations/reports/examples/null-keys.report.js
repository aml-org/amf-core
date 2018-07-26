Model: file://amf-client/shared/src/test/resources/validations/production/null-keys/api.raml
Profile: RAML
Conforms? false
Number of results: 3

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: b should be integer
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/production/null-keys/api.raml#/web-api/end-points/%2FUsuario/delete/request/application%2Fjson/schema/example/default-example
  Property: 
  Position: Some(LexicalInformation([(21,17)-(24,11)]))
  Location: file://amf-client/shared/src/test/resources/validations/production/null-keys/api.raml

Level: Warning

- Source: http://a.ml/vocabularies/amf/parser#parsing-warning
  Message: 'schema' keyword it's deprecated for 1.0 version, should use 'type' instead
  Level: Warning
  Target: 
  Property: 
  Position: Some(LexicalInformation([(11,8)-(11,14)]))
  Location: file://amf-client/shared/src/test/resources/validations/production/null-keys/api.raml

- Source: http://a.ml/vocabularies/amf/parser#parsing-warning
  Message: 'schema' keyword it's deprecated for 1.0 version, should use 'type' instead
  Level: Warning
  Target: 
  Property: 
  Position: Some(LexicalInformation([(20,8)-(20,14)]))
  Location: file://amf-client/shared/src/test/resources/validations/production/null-keys/api.raml
