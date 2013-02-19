
run using embedded elasticsearch:

	mvn exec:java -Dexec.args="-l"

run using elasticsearch TransportClient

	mvn exec:java -Dexec.args="-t clusterName hostName hostPort"

optionally change our http port:

	mvn exec:java -Dexec.args="-l -p 12345"

switch logfile configuration, for example this one, using ansi colors:

	mvn exec:java -Dexec.args="-l -L logback-ansi.xml"

(see degraphmalizer.Options for CLI options)

If you quit with Ctrl-c, the graph and elastic search node with be shutdown
cleanly.

In IDEA, don't press the red square ("Stop") but press the "Exit" button (you
should see "JVM shutdown received").
