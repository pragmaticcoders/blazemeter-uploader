blazemeter-uploader-maven-plugin
============================
A Maven plugin to generate a [JMeter project](http://jmeter.apache.org/) from a [JUnit tests](http://junit.org/junit4/) and/or upload  to a [BlazeMeter](https://www.blazemeter.com/)

Usage
============================

Add to your `build->plugins` section (default phase is `generate-sources` phase)
```xml
<plugin>
      <groupId>com.pragmaticcoders</groupId>
      <artifactId>blazemeter-uploader-maven-plugin</artifactId>
      <version>0.0.1-Snapshot</version>
      <executions>
          <execution>
              <goals>
                  <goal>generate</goal>
              </goals>
          </execution>
      </executions>
      <configuration>                    
          <API_KEY>Your api-key to access BlazeMeter account</API_KEY>
          <TEST_ID>Test id to populate </TEST_ID>
          <jmeterVersion>3.2</jmeterVersion>                    
          <jmeterProjectFileName>jmeter.project.file.name.jmx</jmeterProjectFileName>
          <packageToScan>your.package.to.scan</packageToScan>
          <useEmbeddedJmeter>true</useEmbeddedJmeter>
      </configuration>
  </plugin>
```
Followed by:

```
mvn clean blazemeter-uploader:generate
mvn blazemeter-uploader:upload

or 

mvn clean blazemeter-uploader:generate blazemeter-uploader:upload
```
### General Configuration parameters
- `packageToScan` - package name to look for tests (`required`)

| Parameter | System property | Default value / required| Description |
| ------ | ------ | ------ | ------ |
|`packageToScan`|`package.to.scan`|`required`|Package name to look for tests|
|`jmeterVersion`|`jmeter.version`|`3.2`|JMeter version to generate a project|
|`jmeterHome`|`jmeter.home`|-|JMeter home installation folder|
|`jmeterProjectFileName`|`jmeter.project`|`jUnit2jMeter.jmx`|JMeter project file name|
|`annotationToBeIncluded`|`include.annotation`|`org.junit.Test`|A comma separated list of annotations to filter tests which will be included in the project|
|`annotationToBeExcluded`|`exclude.annotation`|`org.junit.Ignore`|A comma separated list of of annotations to filter tests which will be excluded from the project|
|`jmeterLocalInstallation`|`jmeter.local.dist`|`${project.build.directory}`|JMeter local installation(used to specify the JMeter download location)|
|`useEmbeddedJmeter`|`useEmbeddedJmeter`|`false`|Use embedded JMeter properties to generate project file, no download or local installation required|
|`API_KEY`|`api.key`|`required`|Api-key to access BlazeMeter account|
|`TEST_ID`|`test.id`|`required`|ID for the test to upload artifacts|
