ModelId: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml
Profile: RAML 1.0
Conforms: false
Number of results: 4

Level: Violation

- Constraint: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: should be equal to one of the allowed values
  Severity: Violation
  Target: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-no-params/scalar/schema/example/default-example
  Property: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-no-params/scalar/schema/example/default-example
  Range: [(26,29)-(26,34)]
  Location: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/liba.raml

- Constraint: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: should be equal to one of the allowed values
  Severity: Violation
  Target: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-example-param/scalar/schema/example/default-example
  Property: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-example-param/scalar/schema/example/default-example
  Range: [(29,29)-(29,47)]
  Location: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/liba.raml

- Constraint: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: should be equal to one of the allowed values
  Severity: Violation
  Target: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-type-param/scalar/schema/example/default-example
  Property: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-type-param/scalar/schema/example/default-example
  Range: [(32,29)-(32,34)]
  Location: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/liba.raml

- Constraint: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: should be equal to one of the allowed values
  Severity: Violation
  Target: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-all-params/scalar/schema/example/default-example
  Property: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/input.raml#/web-api/endpoint/%2Fuser/post/request/parameter/header/broken-all-params/scalar/schema/example/default-example
  Range: [(35,29)-(35,47)]
  Location: file://amf-cli/shared/src/test/resources/validations/resource_types/parameterized-references/liba.raml
