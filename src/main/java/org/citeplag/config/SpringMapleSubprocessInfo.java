package org.citeplag.config;

import gov.nist.drmf.interpreter.common.process.ProcessKeys;
import gov.nist.drmf.interpreter.common.process.RmiSubprocessInfo;
import gov.nist.drmf.interpreter.maple.secure.MapleRmiServer;

import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Andre Greiner-Petter
 */
public class SpringMapleSubprocessInfo implements RmiSubprocessInfo {
    private static final String PROP_LAUNCHER_PATH = "org.springframework.boot.loader.PropertiesLauncher";

    @Override
    public String getClassName() {
        return MapleRmiServer.class.getName();
    }

    @Override
    public List<String> getJvmArgs() {
        List<String> list = new LinkedList<>();
        list.add("-XX:HeapDumpPath=/dev/null");
        list.add("-Xms10g");
        list.add("-Xss200M");
        return list;
    }

    @Override
    public List<String> getCommandLineArguments() {
        String javaHome = System.getProperty(ProcessKeys.JAVA_HOME);
        String javaBin = Paths.get(javaHome, "bin", "java").toString();
        String classpath = System.getProperty(ProcessKeys.JAVA_CLASSPATH);

        // Spring is using a different way to load classes. So we need to
        // load the class differently. You need to load it via PropertiesLauncher
        String loader = "-Dloader.main=" + getClassName();

        List<String> command = new LinkedList<>();
        command.add(javaBin);
        command.addAll(this.getJvmArgs());
        command.add(ProcessKeys.JAVA_CLASSPATH_FLAG);
        command.add(classpath);

        // now we need to start things differently. First we must specify the main
        command.add(loader);

        // now we actually start PropertiesLauncher rather than the main class directly
        command.add(PROP_LAUNCHER_PATH);

        return command;
    }
}
