package amf.client.remod

case class InvalidBaseUnitTypeException(currentIri:String, exceptedIri:String) extends Exception(s"Current base unit of type $currentIri is not of expected type $exceptedIri"){

}
