package junit.converter.plugin;

import com.google.common.collect.ImmutableList;
import io.github.lukehutch.fastclasspathscanner.scanner.MethodInfo;
import net.lingala.zip4j.core.ZipFile;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.COMPILE)
public class ProjectGenerationMojo extends AbstractMojo {
    @Parameter(property = "package.to.scan", required = true)
    private String packageToScan;

    @Parameter(property = "jmeter.version", defaultValue = "3.2")
    private String jmeterVersion;

    @Parameter(property = "jmeter.home")
    private String jmeterHome;

    @Parameter(property = "jmeter.project", defaultValue = "jUnit2jMeter.jmx")
    private String jmeterProjectFileName;

    @Parameter(property = "include.annotation")
    private Set<String> annotationToBeIncluded;

    @Parameter(property = "exclude.annotation")
    private Set<String> annotationToBeExcluded;

    @Parameter(property = "jmeter.local.dist", defaultValue = "${project.build.directory}")
    private String jmeterLocalInstallation;

    @Parameter(defaultValue = "${project.build.directory}")
    private File target;

    @Parameter(property = "useEmbeddedJmeter", defaultValue = "false")
    private boolean useEmbeddedJmeter;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    private Map<String, List<MethodInfo>> data = new HashMap<>();

    private String jmeterDefaultVersion = "3.2";
    private String jmeterDefaultInstallation = System.getProperty("user.dir") + File.separator + "target";
    private String jmeterDefaultProjectFileName = "jUnit2jMeter.jmx";

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession mavenSession;
    @Component
    private BuildPluginManager pluginManager;

    public ProjectGenerationMojo() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        prepareSource();
        validateInput();
        scanPackageAndCollectData();
        if (useEmbeddedJmeter) {
            createJmeterEmbeddedDist();
        } else {
            initJmeterInfrastructure();
        }
        generateProjectFile();
    }

    private void generateProjectFile() {
        try {
            new TestPlanGenerator().generate(data, new File(jmeterLocalInstallation), jmeterProjectFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void scanPackageAndCollectData() throws MojoExecutionException {
        Thread.currentThread().setContextClassLoader(getClassLoader());

        data = new JunitScanner()
                .setPackageToScan(packageToScan)
                .setAnnotationToBeExcluded(annotationToBeExcluded)
                .setAnnotationToBeIncluded(annotationToBeIncluded)
                .collectData();
    }

    private ClassLoader getClassLoader() throws MojoExecutionException {
        try {
            List<String> classpathElements = project.getCompileClasspathElements();
            classpathElements.add(project.getBuild().getOutputDirectory());
            classpathElements.add(project.getBuild().getTestOutputDirectory());
            URL urls[] = new URL[classpathElements.size()];

            for (int i = 0; i < classpathElements.size(); ++i) {
                urls[i] = new File(classpathElements.get(i)).toURI().toURL();
            }
            return new URLClassLoader(urls, getClass().getClassLoader());
        } catch (Exception e)//gotta catch em all
        {
            throw new MojoExecutionException("Couldn't create a classloader.", e);
        }
    }

    private void downloadJmeter() throws IOException {

        URL website = new URL("https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-" + jmeterVersion + ".zip");
        HttpURLConnection connection;
        connection = (HttpURLConnection) website.openConnection();
        connection.setRequestMethod("HEAD");
        int code = connection.getResponseCode();
        assert code == 200;

        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        getLog().info("Jmeter download started");
        LocalTime startTime = LocalTime.now();
        String jmeterLocation = jmeterLocalInstallation + File.separator + "apache-jmeter-" + jmeterVersion + ".zip";
        FileOutputStream fos = new FileOutputStream(jmeterLocation);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        LocalTime completeTime = LocalTime.now();
        getLog().info("Jmeter download completed - " + jmeterLocation);
        Duration between = Duration.between(startTime, completeTime);
        getLog().info("Downloaded in : " + between.getSeconds() + " seconds");

    }

    private void unzip() throws net.lingala.zip4j.exception.ZipException {
        String source = jmeterLocalInstallation + File.separator + "apache-jmeter-" + jmeterVersion + ".zip";
        String destination = new File(jmeterLocalInstallation).getAbsolutePath();
        ZipFile zipFile = new ZipFile(source);
        zipFile.extractAll(destination);
        getLog().info("Zip archive is successfully unpacked to :" + destination);
        System.setProperty("jmeter.home", destination + "/apache-jmeter-" + jmeterVersion);
    }

    private void initJmeterInfrastructure() {
        if (!data.isEmpty()) {
            if (jmeterHome == null) {
                try {
                    downloadJmeter();
                    unzip();
                } catch (net.lingala.zip4j.exception.ZipException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (jmeterHome != null) {
                System.setProperty("jmeter.home", jmeterHome);
            }
            getLog().info("JmeterHome is pointed to: " + System.getProperty("jmeter.home"));
        } else {
            getLog().info("There is no tests to be converted to jMeter project");
        }
    }

    private void validateInput() {
        // ----------------------------------------------------------------------
        // jMeter version
        // ----------------------------------------------------------------------
        ImmutableList<String> availableVersions = ImmutableList.of("2.2", "2.6", "2.7", "2.8", "2.9", "2.10", "2.11", "2.12", "2.13", "3.0", "3.1", "3.2");

        if (!availableVersions.contains(jmeterVersion)) {
            getLog().warn(jmeterVersion + " - version is not a valid one from list: " + availableVersions.toString());
            getLog().warn("Default version \"" + jmeterDefaultVersion + "\" will bu used instead");
            jmeterVersion = jmeterDefaultVersion;
        }

        // ----------------------------------------------------------------------
        // Destination file
        // ----------------------------------------------------------------------
        if (!Files.exists(new File(jmeterLocalInstallation).toPath(), LinkOption.NOFOLLOW_LINKS)) {
            getLog().warn("Destination folder for Jmeter installation does not exists");
            getLog().warn("Default folder \"" + jmeterDefaultInstallation + "\" will be use instead");
            jmeterLocalInstallation = jmeterDefaultInstallation;
        }

        // ----------------------------------------------------------------------
        // jMeter project file name
        // ----------------------------------------------------------------------

        if (!Pattern.compile("^.{1,}\\.jmx$").matcher(jmeterProjectFileName).matches()) {
            getLog().warn("\"" + jmeterProjectFileName + "\" is not a correct jmeter project file name");
            getLog().warn("\"" + jmeterDefaultProjectFileName + "\" will be used instead");
            jmeterProjectFileName = jmeterDefaultProjectFileName;
        }
    }

    private void prepareSource() throws MojoExecutionException {
        compileSource();
        compileTestSource();
        generateDependencyJar();
        generateTestJar();

    }

    private void createJmeterEmbeddedDist() {
        String source = "/jmeter/" + jmeterVersion + "/bin/";
        File destination = FileUtils.createDestinationFolder(target);

        FileUtils.getFileList()
                .forEach(file -> FileUtils.copyFile(file, source, destination));

        System.setProperty("jmeter.home", target + File.separator + FileUtils.EMBEDDED_JMETER_HOME);
        getLog().info("JmeterHome is pointed to: " + System.getProperty("jmeter.home"));
    }

    // ----------------------------------------------------------------------
    // Compile source
    // ----------------------------------------------------------------------
    private void compileSource() throws MojoExecutionException {

        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-compiler-plugin"),
                        version("3.6.1")
                ),
                goal(goal("compile")),

                configuration(
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
    // ----------------------------------------------------------------------
    // Compile test source
    // ----------------------------------------------------------------------
    private void compileTestSource() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-compiler-plugin"),
                        version("3.6.1")
                ),
                goal(goal("testCompile")),

                configuration(
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
    // ----------------------------------------------------------------------
    // Generate a jar archive with all dependencies
    // ----------------------------------------------------------------------
    private void generateDependencyJar() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-assembly-plugin"),
                        version("3.0.0")
                ),

                goal("single"),

                configuration(

                        element(name("descriptorRefs"),
                                element(name("descriptorRef"), "jar-with-dependencies"))

                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
    // ----------------------------------------------------------------------
    // Generate jar archive for tests
    // ----------------------------------------------------------------------
    private void generateTestJar() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.apache.maven.plugins"),
                        artifactId("maven-jar-plugin"),
                        version("3.0.2")
                ),
                goal("test-jar"),

                configuration(
                ),
                executionEnvironment(
                        mavenProject,
                        mavenSession,
                        pluginManager
                )
        );
    }
}