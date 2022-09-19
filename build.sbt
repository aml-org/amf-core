import org.scalajs.core.tools.linker.ModuleKind
import sbt.Keys.{libraryDependencies, resolvers}
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtsonar.SonarPlugin.autoImport.sonarProperties

val ivyLocal = Resolver.file("ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

name := "amf-core"
ThisBuild / scalaVersion := "2.12.11"
ThisBuild / version := "5.1.0

publish := {}

jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()

lazy val sonarUrl   = sys.env.getOrElse("SONAR_SERVER_URL", "Not found url.")
lazy val sonarToken = sys.env.getOrElse("SONAR_SERVER_TOKEN", "Not found token.")
lazy val branch     = sys.env.getOrElse("BRANCH_NAME", "develop")

sonarProperties ++= Map(
    "sonar.login"                      -> sonarToken,
    "sonar.projectKey"                 -> "mulesoft.amf-core",
    "sonar.projectName"                -> "AMF-CORE",
    "sonar.projectVersion"             -> "4.0.0",
    "sonar.sourceEncoding"             -> "UTF-8",
    "sonar.github.repository"          -> "aml-org/amf-core",
    "sonar.branch.name"                -> branch,
    "sonar.sources"                    -> "shared/src/main/scala",
    "sonar.tests"                      -> "shared/src/test/scala",
    "sonar.scala.scoverage.reportPath" -> "target/scala-2.12/scoverage-report/scoverage.xml"
)

val settings = Common.settings ++ Common.publish ++ Seq(
    organization := "com.github.amlorg",
    resolvers ++= List(ivyLocal, Common.releases, Common.snapshots, Resolver.mavenLocal, Resolver.mavenCentral),
    credentials ++= Common.credentials(),
    libraryDependencies ++= Seq(
        "org.mule.common" %%% "scala-common-test" % "0.0.10" % Test
    )
)

/** **********************************************
  * AMF-Core
  * ********************************************* */
lazy val workspaceDirectory: File =
  sys.props.get("sbt.mulesoft") match {
    case Some(x) => file(x)
    case _       => Path.userHome / "mulesoft"
  }

val syamlVersion = "1.1.319"

lazy val syamlJVMRef = ProjectRef(workspaceDirectory / "syaml", "syamlJVM")
lazy val syamlJSRef  = ProjectRef(workspaceDirectory / "syaml", "syamlJS")
lazy val syamlLibJVM = "org.mule.syaml" %% "syaml" % syamlVersion
lazy val syamlLibJS  = "org.mule.syaml" %% "syaml_sjs0.6" % syamlVersion

lazy val defaultProfilesGenerationTask = TaskKey[Unit](
    "defaultValidationProfilesGeneration",
    "Generates the validation dialect documents for the standard profiles")

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .settings(
      Seq(
          name := "amf-core"
      ))
  .in(file("."))
  .settings(settings)
  .jvmSettings(
      libraryDependencies += "org.scala-js"           %% "scalajs-stubs"          % scalaJSVersion % "provided",
      libraryDependencies += "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.8.0",
      Compile / packageDoc / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-core-javadoc.jar"
  )
  .jsSettings(
      libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.7",
      scalaJSModuleKind := ModuleKind.CommonJSModule,
      Compile / fullOptJS / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-core-module.js",
      scalacOptions += "-P:scalajs:suppressExportDeprecations"
  )
  .disablePlugins(SonarPlugin)

lazy val coreJVM = core.jvm
  .in(file("./jvm"))
  .sourceDependency(syamlJVMRef, syamlLibJVM)

lazy val coreJS = core.js
  .in(file("./js"))
  .sourceDependency(syamlJSRef, syamlLibJS)
  .disablePlugins(SonarPlugin)

ThisBuild / libraryDependencies ++= Seq(

    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.1" cross CrossVersion.constant("2.12.11")),
    "com.github.ghik" % "silencer-lib" % "1.7.1" % Provided cross CrossVersion.constant("2.12.11")
)
