Model: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml
Profile: RAML 1.0
Conforms? false
Number of results: 4

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#closed-shape
  Message: Properties thisIsWrong not supported in a raml 1.0 shape node
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml#/declarations/types/scalar/ErroneousType
  Property: 
  Position: Some(LexicalInformation([(14,4)-(15,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml

- Source: http://a.ml/vocabularies/amf/parser#closed-shape
  Message: Properties noHolidays not supported in a raml 1.0 unionShape node
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml#/declarations/types/union/Incorrect1
  Property: 
  Position: Some(LexicalInformation([(22,4)-(23,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml

- Source: http://a.ml/vocabularies/amf/parser#closed-shape
  Message: Properties f not supported in a raml 1.0 unionShape node
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml#/declarations/types/union/Incorrect2
  Property: 
  Position: Some(LexicalInformation([(25,4)-(26,0)]))
  Location: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml

- Source: http://a.ml/vocabularies/amf/parser#closed-shape
  Message: Properties error not supported in a raml 1.0 unionShape node
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml#/declarations/types/union/Incorrect3
  Property: 
  Position: Some(LexicalInformation([(28,4)-(28,15)]))
  Location: file://amf-client/shared/src/test/resources/validations/facets/custom-facets.raml
