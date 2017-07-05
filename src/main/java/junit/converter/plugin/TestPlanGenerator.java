package junit.converter.plugin;

import io.github.lukehutch.fastclasspathscanner.scanner.MethodInfo;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.control.gui.TestPlanGui;
import org.apache.jmeter.protocol.java.control.gui.JUnitTestSamplerGui;
import org.apache.jmeter.protocol.java.sampler.JUnitSampler;
import org.apache.jmeter.save.SaveService;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.threads.gui.ThreadGroupGui;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.collections.HashTree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestPlanGenerator {

    public TestPlanGenerator() {
        File jmeterHome = resolveJmeterHome();
        File jmeterProperties = resolveJmeterProperties(jmeterHome);

        JMeterUtils.setJMeterHome(jmeterHome.getPath());
        JMeterUtils.loadJMeterProperties(jmeterProperties.getPath());
        JMeterUtils.initLocale();
    }

    public void generate(Map<String, List<MethodInfo>> data, File targetDir, String fileName) throws IOException {
        List<JUnitSampler> samplers = new ArrayList<>();
        data.entrySet().stream().forEach(clazz -> {
            clazz.getValue().stream().forEach(method -> {
                samplers.add(createJunitSampler(clazz.getKey(), method.getMethodName()));
            });
        });

        ThreadGroup threadGroup = createThreadGroup();
        TestPlan testPlan = createTestPlan();

        HashTree testPlanTree = new HashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(samplers);

        SaveService.saveTree(testPlanTree, new FileOutputStream(new File(targetDir, fileName)));
    }

    private JUnitSampler createJunitSampler(String className, String methodName) {
        JUnitSampler jUnitSampler = new JUnitSampler();
        jUnitSampler.setName(className.substring(className.lastIndexOf('.') + 1) + "-" + methodName);
        jUnitSampler.setClassname(className);
        jUnitSampler.setMethod(methodName);
        jUnitSampler.setSuccess("Test successful");
        jUnitSampler.setSuccessCode("200");
        jUnitSampler.setFailure("Test failed");
        jUnitSampler.setFailureCode("0001");
        jUnitSampler.setError("An unexpected error occured");
        jUnitSampler.setErrorCode("9999");
        jUnitSampler.setJunit4(true);
        jUnitSampler.setProperty(TestElement.TEST_CLASS, JUnitSampler.class.getName());
        jUnitSampler.setProperty(TestElement.GUI_CLASS, JUnitTestSamplerGui.class.getName());
        jUnitSampler.setEnabled(true);
        return jUnitSampler;
    }

    private TestPlan createTestPlan() {
        TestPlan testPlan = new TestPlan("Test Plan");
        testPlan.setProperty(TestElement.TEST_CLASS, TestPlan.class.getName());
        testPlan.setProperty(TestElement.GUI_CLASS, TestPlanGui.class.getName());
        testPlan.setEnabled(true);
        testPlan.setComment("");
        testPlan.setFunctionalMode(false);
        testPlan.setSerialized(false);
        testPlan.setUserDefinedVariables((Arguments) new ArgumentsPanel().createTestElement());
        return testPlan;
    }

    private ThreadGroup createThreadGroup() {
        LoopController loopController = new LoopController();
        loopController.setName("Loop Controller");
        loopController.setLoops(1);
        loopController.setFirst(true);
        loopController.setProperty(TestElement.TEST_CLASS, LoopController.class.getName());
        loopController.setProperty(TestElement.GUI_CLASS, LoopControlPanel.class.getName());
        loopController.setEnabled(true);
        loopController.initialize();

        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Thread Group");
        threadGroup.setNumThreads(1);
        threadGroup.setRampUp(1);
        threadGroup.setSamplerController(loopController);
        threadGroup.setProperty(TestElement.TEST_CLASS, ThreadGroup.class.getName());
        threadGroup.setProperty(TestElement.GUI_CLASS, ThreadGroupGui.class.getName());
        return threadGroup;
    }

    private File resolveJmeterProperties(File jmeterHome) {
        String slash = System.getProperty("file.separator");
        File jmeterProperties = new File(jmeterHome.getPath() + slash + "bin" + slash + "jmeter.properties");
        if (!jmeterProperties.exists()) {
            throw new RuntimeException("jmeter.properties doesn't exists");
        }
        return jmeterProperties;
    }

    private File resolveJmeterHome() {
        File jmeterHome = new File(System.getProperty("jmeter.home"));
        if (!jmeterHome.exists()) {
            throw new RuntimeException("jmeter.home property is not set or pointing to incorrect location");
        }
        return jmeterHome;
    }
}
