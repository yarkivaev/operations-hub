import sbt.*
import sbt.Keys.*

ThisBuild / version := {
  val versionFile = (ThisBuild / baseDirectory).value / "VERSION"
  if (!versionFile.exists()) sys.error("VERSION file missing in operations-hub root")
  IO.read(versionFile).trim
}
ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "yarkivaev"
ThisBuild / organizationName := "yarkivaev"
ThisBuild / publishMavenStyle := true
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / licenses := List("MIT" -> url("https://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/yarkivaev/operations-hub"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/yarkivaev/operations-hub"),
    "scm:git:git@github.com:yarkivaev/operations-hub.git",
  ),
)
ThisBuild / developers := List(
  Developer("yarkivaev", "yarkivaev", "", url("https://github.com/yarkivaev")),
)

resolvers += "maven-central-apache" at "https://repo.maven.apache.org/maven2"

val refinedV = "0.11.2"

ThisBuild / javaOptions ++= Seq(
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "-Dcats.effect.warnOnNonMainThreadDetected=false",
)

lazy val commonSettings = Seq(
  Compile / run / fork := true,
  run / cancelable := true,
  Test / fork := true,
  Test / cancelable := true,
  javaOptions := (ThisBuild / javaOptions).value,
)

lazy val githubOwner = "yarkivaev"
lazy val githubRepo = "operations-hub"
lazy val gitlabMavenHost = "gitlab.scada-cicd.svc.cluster.local"

lazy val publishSettings = Seq(
  publishTo := {
    sys.env.get("CI_API_V4_URL").filter(_.nonEmpty) match {
      case Some(api) =>
        Some(
          ("gitlab-maven" at s"$api/projects/${sys.env("CI_PROJECT_ID")}/packages/maven")
            .withAllowInsecureProtocol(true),
        )
      case None =>
        Some("GitHub Package Registry" at s"https://maven.pkg.github.com/$githubOwner/$githubRepo")
    }
  },
  credentials ++= {
    val gitlab = sys.env.get("CI_JOB_TOKEN").filter(_.nonEmpty).map { token =>
      Credentials("GitLab Packages Registry", gitlabMavenHost, "gitlab-ci-token", token)
    }
    val github = for {
      user <- sys.env.get("GITHUB_ACTOR").filter(_.nonEmpty)
      token <- sys.env.get("GITHUB_TOKEN").filter(_.nonEmpty)
    } yield Credentials("GitHub Package Registry", "maven.pkg.github.com", user, token)
    gitlab.toSeq ++ github.toSeq
  },
  Test / publishArtifact := true,
)

lazy val root =
  (project in file("."))
    .settings(
      commonSettings,
      publishSettings,
      name := "operations-hub",
      libraryDependencies ++= Seq(
        "org.typelevel" %% "cats-core" % "2.12.0",
        "org.typelevel" %% "cats-effect" % "3.5.4",
        "eu.timepit" %% "refined" % refinedV,
        "org.scalameta" %% "munit" % "0.7.29" % Test,
        "org.hamcrest" % "hamcrest" % "2.2" % Test,
      ),
    )

lazy val planExport =
  (project in file("plan-export"))
    .dependsOn(root)
    .settings(
      commonSettings,
      publishSettings,
      name := "operations-hub-plan-export",
      libraryDependencies ++= Seq(
        "org.scalameta" %% "munit" % "0.7.29" % Test,
        "org.hamcrest" % "hamcrest" % "2.2" % Test,
      ),
    )

lazy val planHttp =
  (project in file("plan-http"))
    .dependsOn(root)
    .settings(
      commonSettings,
      publishSettings,
      name := "operations-hub-plan-http",
      Compile / run / mainClass := Some("hub.planhttp.PlanHttpDemoServer"),
      libraryDependencies ++= Seq(
        "org.http4s" %% "http4s-dsl" % "0.23.30",
        "org.http4s" %% "http4s-ember-server" % "0.23.30",
        "org.scalameta" %% "munit" % "0.7.29" % Test,
        "org.hamcrest" % "hamcrest" % "2.2" % Test,
      ),
    )
