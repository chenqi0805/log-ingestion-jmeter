# log-ingestion-jmeter

## Setup on EC2

1. run the following commands in a bash script:

```
# install Java
sudo yum install java-11-amazon-corretto-headless -y
# install jmeter
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.4.1.tgz
tar -xzf apache-jmeter-5.4.1.tgz
wget https://search.maven.org/remotecontent?filepath=kg/apc/jmeter-plugins-manager/1.6/jmeter-plugins-manager-1.6.jar -O apache-jmeter-5.4.1/lib/ext/jmeter-plugins-manager-1.6.jar
# install essential jmeter add-ons
wget http://search.maven.org/remotecontent?filepath=kg/apc/cmdrunner/2.2/cmdrunner-2.2.jar -O apache-jmeter-5.4.1/lib/cmdrunner-2.2.jar
java -cp apache-jmeter-5.4.1/lib/ext/jmeter-plugins-manager-1.6.jar org.jmeterplugins.repository.PluginManagerCMDInstaller
./apache-jmeter-5.4.1/bin/PluginsManagerCMD.sh install jpgc-casutg
./apache-jmeter-5.4.1/bin/PluginsManagerCMD.sh install jpgc-tst
./apache-jmeter-5.4.1/bin/PluginsManagerCMD.sh install jpgc-autostop
# install log ingestion custom sampler client
sudo yum install git -y
git clone https://github.com/chenqi0805/log-ingestion-jmeter.git
cd log-ingestion-jmeter/
./gradlew jar
cd
mv log-ingestion-jmeter/build/libs/log-ingestion-jmeter-1.0-SNAPSHOT.jar apache-jmeter-5.4.1/lib/ext/log-ingestion-jmeter-1.0-SNAPSHOT.jar
```

2. run load generation by CLI execution

- Target request per second

An example CLI execution

```
./apache-jmeter-5.4.1/bin/jmeter -n -tlog-ingestion-jmeter/jmeter_dataprepper_log_plan_steady_configurable_load.jmx -JtargetAddress=<data-prepper-instance-public-ip> -JtargetPort=<data-prepper-http-source-port> -Jrps=20 -f -l log.jtl
```

- Target number of threads

```
./apache-jmeter-5.4.1/bin/jmeter -n -tlog-ingestion-jmeter/jmeter_log_plan_steady_configurable_threads.jmx -JtargetAddress=<data-prepper-instance-public-ip> -Juri=/log/ingest -JtargetPort=<data-prepper-http-source-port> -Jthreads=1000 -f -l log.jtl
```

## Configuration Parameters

Allows configuration by -J:

### Java Request Sampler Client configurations

* `targetAddress`(Required): A string represents data-prepper or logstash instance public endpoint without http or https header.
* `uri`(Optional): A string represents data-prepper or logstash uri path. Defaults to `/log/ingest`. The default value is for data-prepper http source. For logstash, change it into `/`.
* `ssl`(Optional): A boolean variable. `true` represents https. `false` represents http. Defaults to `false`.
* `timeoutMs`(Optional): A number represents client request timeout in millis. Defaults to `10_000`.
* `targetPort`(Optional): A number represents the port data-prepper or logstash is listening on. Defaults to `2021`.
* `numLogs`(Optional): A number represents number of apache log jsons in the request body of a sample http request. Defaults to 20. Each apache log json takes the format

```
{"message": "<random apache log>"}
```

### JMeter load generation configuration

#### Target rps test plan

The test plans `jmeter_dataprepper_log_plan_steady_configurable_load.jmx` or `jmeter_logstash_log_plan_steady_configurable_load.jmx` tries to shoot a target rps:

* `rps`(Required): A number represents the target number of requests per second in [Throughput Shaping Timer](https://jmeter-plugins.org/wiki/ThroughputShapingTimer/)
* `duration`(Optional): A number represents total seconds of producing the above `rps`. Defaults to 3500.

Note: 
1. In Throughput Shaping Timer RPS schedule, the first 60 seconds is used to achieve the target rps. So if the duration is set to be 300. The total duration will be 360 seconds.
2. Under the hood, [Concurrency Thread Group](https://jmeter-plugins.org/wiki/ConcurrencyThreadGroup/) is used to produce the target rps. The target concurrency is set by [Schedule Feedback Function](https://jmeter-plugins.org/wiki/ThroughputShapingTimer/#Schedule-Feedback-Function)
to be `${__tstFeedback(TPSShapingTimer, 1, 2000, 10)}`. The Hold Target Rate Time is set to be greater than the total duration in Throughput Shaping Timer schedule.

#### Target users(threads) test plan

The test plan `jmeter_log_plan_steady_configurable_threads.jmx` allows configuring fixed number of users/threads in jmeter thread group to increase the load sending to the server:

* `threads` (Optional): A number represents the number of threads Jmeter will use to generate the load (requests per second) to execute the test. 
* `duration`(Optional): A number represents total seconds of producing the above `rps`. Defaults to 3500.