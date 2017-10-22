package io.sealights.plugins.sealightsjenkins.integration.plugins;

import io.sealights.plugins.sealightsjenkins.ExecutionType;
import io.sealights.plugins.sealightsjenkins.integration.PomFile;
import io.sealights.plugins.sealightsjenkins.integration.SeaLightsPluginInfo;
import io.sealights.plugins.sealightsjenkins.utils.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static io.sealights.plugins.sealightsjenkins.utils.StringUtils.isNullOrEmpty;

/**
 * This class helps add the sealights-maven plugin to a pom file.
 */
public class SealightsMavenPluginIntegrator extends PluginIntegrator {
    private static final String METADATA_KEY = "metadata";
    private static final String SEALIGHTS_JVM_PARAMS_KEY = "sealightsJvmParams";
    private static final String BUILD_SCANNER_PARAMS_KEY="buildScannerParams";

    private String overridePluginVersion;
    private SeaLightsPluginInfo pluginInfo;
    private Document pomDoc;


    public SealightsMavenPluginIntegrator
            (Logger logger, SeaLightsPluginInfo pluginInfo, String overridePluginVersion, PomFile pomFile) {
        super(logger, pomFile);
        this.pluginInfo = pluginInfo;
        this.overridePluginVersion = overridePluginVersion;
        this.pomDoc = pomFile.getDocument();
    }

    private String toPluginText() {

        StringBuilder plugin = new StringBuilder();
        plugin.append("<groupId>" + groupId() + "</groupId>");
        plugin.append("<artifactId>" + artifactId() + "</artifactId>");
        if (!isNullOrEmpty(overridePluginVersion)) {
            plugin.append("<version>" + overridePluginVersion + "</version>");
        }

        plugin = addConfigurationToPluginText(plugin);
        plugin = addExecutionsToPluginText(plugin);

        return plugin.toString();
    }

    private StringBuilder addConfigurationToPluginText(StringBuilder plugin) {
        plugin.append("<configuration>");

        if (!pluginInfo.isEnabled()) {
            plugin.append("<enable>false</enable>");
        }

        if (pluginInfo.getTokenData() != null){
            tryAppendValue(plugin, pluginInfo.getTokenData().getToken(), "token");
        }else{
            tryAppendValue(plugin, pluginInfo.getCustomerId(), "customerid");
            tryAppendValue(plugin, pluginInfo.getServerUrl(), "server");
        }

        tryAppendValue(plugin, pluginInfo.getProxy(), "proxy");

        tryAppendValue(plugin, pluginInfo.getBuildSessionId(), "buildSessionId");
        tryAppendValue(plugin, String.valueOf(pluginInfo.isCreateBuildSessionId()), "createBuildSessionId");

        String appName = pluginInfo.getAppName();
        if ("Build Per Module".equalsIgnoreCase(pluginInfo.getBuildStrategy().getDisplayName())) {
            appName = "[" + pluginInfo.getAppName() + "] - " + pluginInfo.getModuleName();
        }

        tryAppendValue(plugin, appName, "appName");
        tryAppendValue(plugin, pluginInfo.getModuleName(), "moduleName");
        tryAppendValue(plugin, pluginInfo.getWorkspacepath(), "workspacepath");
        tryAppendValue(plugin, pluginInfo.getBuildName(), "build");
        tryAppendValue(plugin, pluginInfo.getBranchName(), "branch");
        tryAppendValue(plugin, pluginInfo.getPackagesIncluded(), "packagesincluded");

        if (!isNullOrEmpty(pluginInfo.getPackagesExcluded())) {
            plugin.append("<packagesexcluded>*FastClassByGuice*, *ByCGLIB*, *EnhancerByMockitoWithCGLIB*, *EnhancerBySpringCGLIB*, " + pluginInfo.getPackagesExcluded() + "</packagesexcluded>");
        }

        if (!isNullOrEmpty(pluginInfo.getClassLoadersExcluded())) {
            plugin.append("<classLoadersExcluded>org.powermock.core.classloader.MockClassLoader, " + pluginInfo.getClassLoadersExcluded() + "</classLoadersExcluded>");
        }

        tryAppendValue(plugin, pluginInfo.getFilesIncluded(), "filesincluded");
        tryAppendValue(plugin, pluginInfo.getScannerJar(), "buildScannerJar");
        tryAppendValue(plugin, pluginInfo.getListenerJar(), "testListenerJar");
        tryAppendValue(plugin, pluginInfo.getFilesStorage(), "filesStorage");
        tryAppendValue(plugin, pluginInfo.getListenerConfigFile(), "testListenerConfigFile");
        tryAppendValue(plugin, pluginInfo.getEnvironment(), "environment");
        tryAppendValue(plugin, pluginInfo.getTestStage(), "testStage");
        tryAppendValue(plugin, pluginInfo.getLabId(), "labId");
        tryAppendValue(plugin, pluginInfo.getFilesExcluded(), "filesexcluded");

        tryAppendValue(plugin, pluginInfo.getOverrideMetaJsonPath(), "overrideMetaJsonPath");
        tryAppendValue(plugin, pluginInfo.getOverrideTestListenerPath(), "overrideTestListenerPath");

        if (!pluginInfo.isRecursive()) {
            plugin.append("<recursive>false</recursive>");
        }

        if (pluginInfo.isLogEnabled()) {
            plugin.append("<logEnabled>true</logEnabled>");
        }
        String logLevel = pluginInfo.getLogLevel().name();
        tryAppendValue(plugin, logLevel, "logLevel");

        if (pluginInfo.getLogDestination() != null && "file".equalsIgnoreCase(pluginInfo.getLogDestination().name())) {
            plugin.append("<logToFile>true</logToFile>");
        }

        tryAppendValue(plugin, pluginInfo.getLogFolder(), "logFolder");

        if(pluginInfo.getMetadata()!=null) {
            plugin = addMetadataToConfigurationInPluginText(plugin);
        }
        if(pluginInfo.getSelightsJvmParams()!=null) {
            plugin = addJvmParamsToConfigurationInPluginText(plugin);
        }
        if(pluginInfo.getBuildScannerParams()!=null) {
            plugin = addBuildScannerParamsToConfigurationInPluginText(plugin);
        }

        plugin.append("</configuration>");

        return plugin;
    }

    private StringBuilder addMetadataToConfigurationInPluginText(StringBuilder plugin) {
        Map<String, String> metadata = new TreeMap<String, String>(pluginInfo.getMetadata());
        return addMapConfigurationToPluginText(plugin,  METADATA_KEY, metadata);
    }

    private StringBuilder addJvmParamsToConfigurationInPluginText(StringBuilder plugin) {
        Map<String, String> jvmParams = new TreeMap<String, String>(pluginInfo.getSelightsJvmParams());
        return addMapConfigurationToPluginText(plugin,SEALIGHTS_JVM_PARAMS_KEY , jvmParams);
    }

    private StringBuilder addBuildScannerParamsToConfigurationInPluginText(StringBuilder plugin) {
        Map<String, String> jvmParams = new TreeMap<String, String>(pluginInfo.getBuildScannerParams());
        return addMapConfigurationToPluginText(plugin,BUILD_SCANNER_PARAMS_KEY , jvmParams);
    }

    private StringBuilder addMapConfigurationToPluginText(StringBuilder plugin, String key, Map<String, String> configMap) {
        if (!(configMap == null || configMap.isEmpty())) {
            plugin.append("<" + key +">");
            Iterator it = configMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> pair = (Map.Entry) it.next();
                tryAppendValue(plugin, pair.getValue(), pair.getKey());
            }
            plugin.append("</" +key + ">");
        }

        return plugin;
    }

    private StringBuilder addExecutionsToPluginText(StringBuilder plugin) {
        plugin.append("<executions>");

        boolean shouldExecuteScanner = ExecutionType.FULL.equals(pluginInfo.getExecutionType());
        if (shouldExecuteScanner)
            appendExecution(plugin, "a1", "build-scanner");
        appendExecution(plugin, "a2", "test-listener");
        plugin.append("</executions>");

        return plugin;
    }

    private void appendExecution(StringBuilder stringBuilder, String executionId, String goal) {
        stringBuilder.append("<execution>");
        stringBuilder.append("<id>" + executionId + "</id>");
        stringBuilder.append("<goals>");
        stringBuilder.append("<goal>" + goal + "</goal>");
        stringBuilder.append("</goals>");
        stringBuilder.append("</execution>");
    }

    private void tryAppendValue(StringBuilder stringBuilder, String value, String elementName) {
        if (!isNullOrEmpty(value)) {
            stringBuilder.append("<" + elementName + ">");
            stringBuilder.append(value);
            stringBuilder.append("</" + elementName + ">");
        }
    }

    @Override
    public String artifactId() {
        return "sealights-maven-plugin";
    }

    @Override
    public String groupId() {
        return "io.sealights.on-premise.agents.plugin";
    }

    @Override
    protected void integrate() throws XPathExpressionException {
        String pluginBodyAsXml = toPluginText();

        integrate(pluginBodyAsXml, pomDoc.getDocumentElement());
        integrateToAllProfiles(pluginBodyAsXml, pomDoc.getDocumentElement());
        logger.debug("Integrated to plugin '" + pluginDescriptor() + "'.");
    }

    @Override
    public boolean isAlreadyIntegrated() {
        return exists();
    }

    private void integrateToAllProfiles(String pluginBodyAsXml, Element parent) throws XPathExpressionException {
        List<Element> profilesList = pomFile.getElements("profiles", parent);
        for (Element profiles : profilesList) {
            List<Element> profileList = pomFile.getElements("profile", profiles);
            for (Element profile : profileList) {
                integrate(pluginBodyAsXml, profile);
            }
        }
    }

    private void integrate(String pluginBodyAsXml, Element parent) throws XPathExpressionException {
        List<Element> buildElements = pomFile.getOrCreateElements("build", parent);

        for (Element buildElement : buildElements) {
            pomFile.verifyPluginsElement(pluginBodyAsXml, buildElement);

            if (pomFile.isNodeExist("pluginManagement", buildElement)) {
                List<Element> pluginManagementElements = pomFile.getOrCreateElements("pluginManagement", buildElement);
                pomFile.verifyPluginsElement(pluginBodyAsXml, pluginManagementElements.get(0));
            }
        }
    }

}
