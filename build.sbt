scalaVersion := "2.12.1"

scalacOptions += "-Ypartial-unification"

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.3")

libraryDependencies += "org.atnos" %% "eff" % "4.1.0"