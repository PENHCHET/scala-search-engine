name := "se-crawler"

version := "0.1"

scalaVersion := "2.11.5"

resolvers ++= Seq(
	"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
	"Spray Repository" at "http://repo.spray.io"
)

libraryDependencies ++= {
	Seq(
  	"com.typesafe.akka"      %% "akka-actor"                    % "2.3.9",
		"com.typesafe.akka"      %% "akka-remote"                   % "2.3.9",
		"io.spray"               %% "spray-client"                  % "1.3.2",
		"com.websudos"           %% "phantom-dsl"                   % "1.5.0",
		"com.websudos"           %% "phantom-zookeeper"             % "1.5.0",
		"org.jsoup"              % "jsoup"                          % "1.8.1",
		"joda-time"              % "joda-time"                      % "2.7"
	)
}
