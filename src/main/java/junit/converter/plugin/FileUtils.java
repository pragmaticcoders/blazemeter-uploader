package junit.converter.plugin;

import com.google.common.collect.ImmutableList;
import org.codehaus.plexus.util.IOUtil;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {
    static final String EMBEDDED_JMETER_HOME = "jmeterHome";

    static void copyFile(String file, String source, File destination) {

        InputStream resource = FileUtils.class.getResourceAsStream(source + file);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(new File(destination, file));
            IOUtil.copy(resource, fileOutputStream);
            fileOutputStream.close();
            fileOutputStream = null;
            resource.close();
            resource = null;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static File createDestinationFolder(File target) {
        File targetJmeterFolder = new File(target, EMBEDDED_JMETER_HOME + "/bin");
        if (targetJmeterFolder.exists() && targetJmeterFolder.isFile()) {
            targetJmeterFolder.delete();
        }
        targetJmeterFolder.mkdirs();
        return targetJmeterFolder;
    }

    static ImmutableList<String> getFileList() {
        return ImmutableList.of
                (
                        "jmeter.properties",
                        "reportgenerator.properties",
                        "saveservice.properties",
                        "system.properties",
                        "upgrade.properties",
                        "user.properties"
                );
    }

    public static List<Path> getFilesListToUpload(File targetDir){
        List<Path> filesToUpload = new ArrayList<>();
        try{
        Files.list(Paths.get(targetDir.getPath()))
                .filter(Files::isRegularFile)
                .filter(path ->
                        path.toString().endsWith("-jar-with-dependencies.jar") |
                                path.toString().endsWith("-tests.jar") |
                                path.toString().endsWith(".jmx")).
                collect(Collectors.toList());
    } catch (IOException e) {
        e.printStackTrace();
    }
    return filesToUpload;
    }
}
