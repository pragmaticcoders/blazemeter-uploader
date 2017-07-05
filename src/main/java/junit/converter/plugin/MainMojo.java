package junit.converter.plugin;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.github.lukehutch.fastclasspathscanner.scanner.MethodInfo;
import net.lingala.zip4j.core.ZipFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Mojo(name = "uploadArtifacts", requiresDependencyResolution = ResolutionScope.COMPILE)
public class MainMojo
        extends AbstractMojo {
    @Parameter(property = "api.key")
    private String API_KEY;

    @Parameter(property = "test.id")
    private String TEST_ID;

    @Parameter(property = "package.to.scan", required = true)
    private String packageToScan;

    @Parameter(property = "jmeter.version", defaultValue = "3.2")
    private String jmeterVersion;

    @Parameter(property = "jmeter.home")
    private String jmeterHome;

    @Parameter(property = "jmeter.file", defaultValue = "jUnit2jMeter.jmx")
    private String jmeterProjectFileName;

    @Parameter(property = "jars.to.upload")
    private List<String> jarsToUpload;

    @Parameter(property = "include.annotation")
    private Set<String> annotationToBeIncluded;

    @Parameter(property = "exclude.annotation")
    private Set<String> annotationToBeExcluded;

    @Parameter(defaultValue = "${project.build.directory}")
    private File target;

    @Parameter(property = "generateOnly", defaultValue = "true")
    private boolean generateOnly;

    @Parameter(property = "uploadOnly", defaultValue = "false")
    private boolean uploadOnly;

    @Component
    public MavenProject project;

    private Map configuration;

    private Map<String, List<MethodInfo>> data = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException {

        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClassLoader());
        data = new JunitScanner()
                .setPackageToScan(packageToScan)
                .setAnnotationToBeExcluded(annotationToBeExcluded)
                .setAnnotationToBeIncluded(annotationToBeIncluded)
                .collectData();

        init();

        try {
            new TestPlanGenerator().generate(data, target, jmeterProjectFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<Path> filesToUpload = new ArrayList<>();
        try {
            filesToUpload = Files.list(Paths.get(target.getPath()))
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith("-jar-with-dependencies.jar") |
                                    path.toString().endsWith("-tests.jar") |
                                    path.toString().endsWith(".jmx")).
                            collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileUploader fileUploader = new FileUploader().apiKey(API_KEY).testId(TEST_ID);
        filesToUpload.stream().forEach(path -> fileUploader.uploadFile(path.toFile()));


    }

    ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();
            classpathElements.add(project.getBuild().getOutputDirectory());
            classpathElements.add(project.getBuild().getTestOutputDirectory());
            URL urls[] = new URL[classpathElements.size()];

            for (int i = 0; i < classpathElements.size(); ++i) {
                urls[i] = new File((String) classpathElements.get(i)).toURI().toURL();
            }
            return new URLClassLoader(urls, getClass().getClassLoader());
        } catch (Exception e)//gotta catch em all
        {
            throw new MojoExecutionException("Couldn't create a classloader.", e);
        }
    }

    private void donwloadJmeter() throws IOException {
        URL website = new URL("https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-" + jmeterVersion + ".zip");

        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        System.out.println("Jmeter download started");
        LocalTime startTime = LocalTime.now();
        String jmeterLocation = target + File.separator + "apache-jmeter-" + jmeterVersion + ".zip";
        FileOutputStream fos = new FileOutputStream(jmeterLocation);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        LocalTime completeTime = LocalTime.now();
        System.out.println("Jmeter download completed - " + jmeterLocation);
        Duration between = Duration.between(startTime, completeTime);
        System.out.println("Downloaded in : " + between.getSeconds() + " seconds");
    }

    public void unzip() throws net.lingala.zip4j.exception.ZipException {
        String source = target + File.separator + "apache-jmeter-" + jmeterVersion + ".zip";
        String destination = target.getAbsolutePath();
        ZipFile zipFile = new ZipFile(source);
        zipFile.extractAll(destination);
        getLog().info("Zip archive is successfully unpacked to :" + destination);
        getLog().info("jmeter home property :" + destination + "/apache-jmeter-" + jmeterVersion);

        System.setProperty("jmeter.home", destination + "/apache-jmeter-" + jmeterVersion);
    }

    private void init() {
        if (!data.isEmpty()) {
            if (jmeterHome == null) {
                try {
                    donwloadJmeter();
                    unzip();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (net.lingala.zip4j.exception.ZipException e) {
                    e.printStackTrace();
                }
            } else if (jmeterHome != null) {
                System.setProperty("jmeter.home", jmeterHome);
            }
        } else {
            getLog().info("There is no tests to be converted to jMeter project");
        }
    }
}
