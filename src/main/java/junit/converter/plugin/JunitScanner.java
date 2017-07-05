package junit.converter.plugin;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ClassInfo;
import io.github.lukehutch.fastclasspathscanner.scanner.MethodInfo;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;

import java.util.*;
import java.util.stream.Collectors;

public class JunitScanner {

    private Map<String, List<MethodInfo>> data = new HashMap<>();
    private ScanResult scanResult;
    private static Set<String> annotationToBeIncluded = new HashSet<>(Arrays.asList("org.junit.Test"));
    private static Set<String> annotationToBeExcluded = new HashSet<>(Arrays.asList("org.junit.Ignore"));
    private String packageToScan;

    public JunitScanner setAnnotationToBeIncluded(Set<String> include) {
        this.annotationToBeIncluded.addAll(include);
        return this;
    }

    public JunitScanner setAnnotationToBeExcluded(Set<String> exclude) {
        this.annotationToBeExcluded.addAll(exclude);
        return this;
    }

    public JunitScanner setPackageToScan(String packageToScan) {
        this.packageToScan = packageToScan;
        return this;
    }

    public Map<String, List<MethodInfo>> collectData() {
        scanPackage(packageToScan);
        List<String> ignoredClasses = scanResult.getNamesOfClassesWithAnnotation("ogr.junit.Ignore").stream().sorted().collect(Collectors.toList());
        List<String> testClasses = scanResult.getNamesOfClassesWithMethodAnnotation("org.junit.Test").stream().sorted().collect(Collectors.toList());
        testClasses.removeAll(ignoredClasses);
        testClasses.stream().forEach(clazz -> {
            getDataFromClassInfo(clazz);
        });
        return data;
    }

    private void getDataFromClassInfo(String className) {
        ClassInfo classInfo = scanResult.getClassNameToClassInfo().get(className);
        List<MethodInfo> collect = classInfo
                .getMethodInfo()
                .stream()
                .filter(method -> {
                    return !method.isConstructor()
                            && method.getAnnotationNames().containsAll(annotationToBeIncluded)
                            && !method.getAnnotationNames().containsAll(annotationToBeExcluded);
                })
                .collect(Collectors.toList());
        if (!collect.isEmpty()) {
            data.put(classInfo.getClassName(), collect);
        }
    }

    private void scanPackage(String packageName) {
        scanResult = new FastClasspathScanner(packageName)
                .ignoreFieldVisibility()
                .enableFieldAnnotationIndexing()
                .enableMethodAnnotationIndexing()
                .enableMethodInfo()
                .enableFieldInfo()
                .scan();
    }

    public List<String> getNamesOfClassesWithMethodAnnotationsByClass(final List<Class> annotations) {
        List<String> annotationList = annotations.stream().map(annotation -> annotation.getName()).collect(Collectors.toList());
        return getNamesOfClassesWithMethodAnnotationsByName(annotationList);
    }

    public List<String> getNamesOfClassesWithMethodAnnotationsByName(final List<String> annotationNameList) {
        Set<String> annotatedClasses = new HashSet<>();
        annotationNameList.stream()
                .map(annotationName -> scanResult.getNamesOfClassesWithMethodAnnotation(annotationName).stream().sorted().collect(Collectors.toSet()))
                .forEach(classList -> annotatedClasses.addAll(classList));
        return new ArrayList<String>(annotatedClasses);
    }
}
