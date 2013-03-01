# Installation

So you want to have your very own Degraphmalizer setup? You've come to the right place!

There are various ways to use it.

 * You can embed the degraphmalizer in your own application, the
   degraphmalizer is responsible for indexing the documents.
   
 * You can index into ES and (using our plugin) notify a standalone
   degraphmalizer instance of those index actions. It will then pull
   the documents from ES, mutate the graph and write the results to
   another ES index.
   
This last mode is described here.

## Degraphmalizer + ES notifications

Setting up the Degraphmalizer and ES with notifications from elasticsearch
that a document has changed. You need four things:

  1. Elasticsearch
  2. `degraphmalizer-elasticsearch-plugin` installed in elasticsearch
  3. `degraphmalizer-core`
  4. Configuration files that tell degraphmalizer-core what to do

# Build

Build requirements:

* [Oracle JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 6 (since Neo4j is not yet compatible with Java 7)
* [Maven](http://maven.apache.org)

Run `mvn package` to build the artifacts. You'll find the resulting jar files in the `target` directories of the subdirectories of the modules.

# Installation

## Install elasticsearch

If you don't have an elasticsearch installation yet you can [download elasticsearch](http://www.elasticsearch.org/download/) and extract the archive. Use `~/opt/elasticsearch/` for example.

## Install degraphmalizer-elasticsearch-plugin

Unzip the degraphmalizer-elasticsearch-plugin jar with dependencies into a subdirectory in your elasticsearch plugins directory (e.g. `~/opt/elasticsearch/plugins/degraphmalizer`). You should get something like this:

    ~/opt/elasticsearch/plugins/degraphmalizer/
    |-- commons-codec-1.6.jar
    |-- commons-logging-1.1.1.jar
    |-- elasticsearch-degraphmalizer-0.20.2-0.1-SNAPSHOT.jar
    |-- httpclient-4.2.3.jar
    `-- httpcore-4.2.2.jar

## Install degraphmalizer-core

TODO
- Put degraphmalizer-core jar with dedepencies in some directory

# Configuration

Alright, all components have been installed. Now you might want to configure a few things. Read on.

## Configure degraphmalizer-elasticsearch-plugin

You can configure the Degraphmalizer plugin using the following settings in the elasticsearch configuration file `elasticsearch.yml`:

`plugin.degraphmalizer.DegraphmalizerPlugin.degraphmalizerScheme`

- URI scheme used to access the Degraphmalizer, either `http` or `https`
- Default: `http`

`plugin.degraphmalizer.DegraphmalizerPlugin.degraphmalizerHost`

- Hostname used to access the Degraphmalizer
- Default: `localhost`

`plugin.degraphmalizer.DegraphmalizerPlugin.degraphmalizerPort`

- Port used to access the Degraphmalizer
- Default: `9821`

`plugin.degraphmalizer.DegraphmalizerPlugin.delayOnFailureInMillis`

- Delay in milliseconds before retrying failed requests to the Degraphmalizer
- Default: `10000`

`plugin.degraphmalizer.DegraphmalizerPlugin.queueLimit`

- Number of updates to queue in memory per index before spooling to disk
- Default: `100000`

`plugin.degraphmalizer.DegraphmalizerPlugin.logPath`

- Path for error logs and overflow spool files
- Default: `/export/elasticsearch/degraphmalizer`

`plugin.degraphmalizer.DegraphmalizerPlugin.maxRetries`

- Number of times to retry sending an update to the Degraphmalizer before considering it failed
- Default: `10`

## Configure degraphmalizer-core

TODO
- Add Degraphmalizer configuration files to tell degraphmalizer-core what to do

# Running

Everything has been installed, it's all configured, let's run this thing!

## Running elasticsearch

For testing you can then start Elasticsearch with the "Run in foreground" option, like this: cd ~/opt/elasticsearch; bin/elasticsearch -f

You then see the log output on the console, `ctrl+c` to quit the elasticsearch node. Without `-f` elasticsearch starts as a background daemon, which is recommended for production.

## Running degraphmalizer-core

- Explain flags / `--help`

## Running with maven:

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
