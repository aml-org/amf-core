package amf.client.remod.amfcore.config

private[amf] case class AMFOptions(parsingOptions: ParsingOptions,
                                   renderOptions: RenderOptions /*, private[amf] var env:AmfEnvironment*/ ) {
  //  def withPrettyPrint(): AmfEnvironment = {
  //    val copied = copy(renderingOptions = renderingOptions.withPrettyPrint)
  //    val newEnv = env.copy(options = copied)
  //    copied.env = newEnv
  //    newEnv
  //  }
}

object AMFOptions {
  def default() = new AMFOptions(ParsingOptions(), RenderOptions())
}
