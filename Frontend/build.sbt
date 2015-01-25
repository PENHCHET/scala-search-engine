name := "se-frontend"

version := "0.1"

scalaVersion := "2.11.5"

resolvers ++= Seq(
	"Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
	"Bintray Russian Morph" at "http://dl.bintray.com/imotov/elasticsearch-plugins",
	"Rediscala" at "http://dl.bintray.com/etaty/maven",
	"Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
)
libraryDependencies ++= {
	Seq(
  	"com.typesafe.akka"            %% "akka-actor"                    % "2.3.9",
		"com.typesafe.akka"            %% "akka-remote"                   % "2.3.9",
		"io.spray"                     %% "spray-can"                     % "1.3.2",// excludeAll(ExclusionRule(organization = "org.parboiled")),
		"io.spray"                     %% "spray-routing-shapeless2"      % "1.3.2",// excludeAll(ExclusionRule(organization = "org.parboiled"), ExclusionRule(organization = "com.chuusai")),
		"io.spray"                     %% "spray-json"                    % "1.3.1",
		"com.websudos"                 %% "phantom-dsl"                   % "1.5.0",
		"com.websudos"                 %% "phantom-zookeeper"             % "1.5.0",
		"com.etaty.rediscala"          %% "rediscala"                     % "1.4.2",
		"org.parboiled"                %% "parboiled"                     % "2.0.1",
		"org.motovs.lucene.morphology" %  "russian"                       % "4.6.0",
		"org.motovs.lucene.morphology" %  "english"                       % "4.6.0",
		"org.apache.lucene"            %  "lucene-analyzers-icu"          % "4.10.2"
	)
}
