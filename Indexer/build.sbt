name := "se-indexer"

version := "0.1"

scalaVersion := "2.11.5"

resolvers ++= Seq(
	"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
	"Bintray Russian Morph" at "http://dl.bintray.com/imotov/elasticsearch-plugins"
)

libraryDependencies ++= {
	Seq(
  	"com.typesafe.akka"            %% "akka-actor"                    % "2.3.9",
		"com.typesafe.akka"            %% "akka-remote"                   % "2.3.9",
		"com.websudos"                 %% "phantom-dsl"                   % "1.5.0",
		"com.websudos"                 %% "phantom-zookeeper"             % "1.5.0",
		"org.motovs.lucene.morphology" %  "russian"                       % "4.6.0",
		"org.motovs.lucene.morphology" %  "english"                       % "4.6.0",
		"org.apache.lucene"            %  "lucene-analyzers-icu"          % "4.10.2"
	)
}
