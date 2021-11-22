# log-ingestion-jmeter

## Setup on EC2

run the following commands in a bash script:

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

An example CLI execution
```
./apache-jmeter-5.4.1/bin/jmeter -n -tlog-ingestion-jmeter/jmeter_dataprepper_log_plan_steady_configurable_load.jmx -JtargetAddress=<data-prepper-instance-public-ip> -JtargetPort=<data-prepper-http-source-port> -Jrps=20 -f -l log.jtl
```
