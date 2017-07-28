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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mojo(name = "upload", requiresDependencyResolution = ResolutionScope.COMPILE)
public class UploadArtifactsMojo
        extends AbstractMojo {
    @Parameter(property = "api.key")
    private String API_KEY;

    @Parameter(property = "test.id")
    private String TEST_ID;

    @Parameter(defaultValue = "${project.build.directory}")
    private File target;

    private Map<String, List<MethodInfo>> data = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException {

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
        filesToUpload.stream().forEach(path -> fileUploader.uploadFile(path.toFile(),getLog()));
    }
}
