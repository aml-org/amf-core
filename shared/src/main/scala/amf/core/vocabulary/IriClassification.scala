package amf.core.vocabulary

sealed trait IriType
case object RelativeIri extends IriType
case object AbsoluteIri extends IriType

/**
  * Simplified version of the spec 'ABNF for IRI References and IRIs' in https://www.ietf.org/rfc/rfc3987.txt
  */
object IriClassification {
  def classify(iri: String): IriType = {
    if (iri.contains("://")) {
      AbsoluteIri
    } else {
      RelativeIri
    }
  }

}
