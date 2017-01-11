package io.sealights.plugins.sealightsjenkins.buildsteps.commands;

import hudson.*;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import io.sealights.plugins.sealightsjenkins.BeginAnalysis;
import io.sealights.plugins.sealightsjenkins.buildsteps.commands.entities.BaseCommandArguments;
import io.sealights.plugins.sealightsjenkins.buildsteps.commands.entities.ExternalReportArguments;
import io.sealights.plugins.sealightsjenkins.buildsteps.commands.entities.SealightsBuildStatus;
import io.sealights.plugins.sealightsjenkins.buildsteps.commands.utils.BuildNameResolver;
import io.sealights.plugins.sealightsjenkins.entities.TokenData;
import io.sealights.plugins.sealightsjenkins.entities.ValidationError;
import io.sealights.plugins.sealightsjenkins.utils.*;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

import java.io.File;
import java.util.*;

/**
 * Created by shahar on 1/9/2017.
 */
public class BuildStatusNotifier extends Notifier {

    private boolean enabled;
    private String buildSessionId;
    private String appName;
    private String branchName;
    private CommandBuildName buildName;
    private String additionalArguments;
    private BeginAnalysis beginAnalysis = new BeginAnalysis();

    @DataBoundConstructor
    public BuildStatusNotifier(boolean enabled, String buildSessionId, String appName, String branchName,
                               CommandBuildName buildName, String additionalArguments) {
        super();
        this.enabled = enabled;
        this.buildSessionId = buildSessionId;
        this.appName = appName;
        this.branchName = branchName;
        this.buildName = buildName;
        this.additionalArguments = additionalArguments;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        Logger logger = new Logger(listener.getLogger(), "[SeaLights Build Status Notifier] ");
        try {
            // This step must be first
            setDefaultValues();
            if (!enabled) {
                logger.info("'Report Build Status' is disabled.");
                return true;
            }

            Properties additionalProps = PropertiesUtils.toProperties(additionalArguments);
            EnvVars envVars = build.getEnvironment(listener);

            String filesStorage = resolveFilesStorage(additionalProps, envVars);

            String reportFile = resolveReportFile(build, envVars, additionalProps, logger);

            ExternalReportArguments externalReportArguments =
                    createExternalReportArguments(envVars, build, additionalProps, reportFile, logger);

            BaseCommandArguments baseCommandArguments = createBaseCommandArguments(
                    envVars, additionalProps, externalReportArguments, logger);
            externalReportArguments.setBaseArgs(baseCommandArguments);

            logger.info("About to report build status.");
            ListenerCommandHandler listenerCommandHandler = new ListenerCommandHandler(baseCommandArguments, filesStorage, logger);
            listenerCommandHandler.handleExternalReport(externalReportArguments);

        } catch (Exception e) {
            logger.error("Failed to send build status report. Error: ", e);
        }

        return true;
    }

    private void setDefaultValues() {
        if (this.buildName == null)
            this.buildName = new CommandBuildName.EmptyBuildName();
    }

    private String resolveReportFile(
            final AbstractBuild<?, ?> build, EnvVars envVars, Properties additionalProps, Logger logger) {

        String reportFile = resolveEnvVar(envVars, (String) additionalProps.get("report"));
        boolean isReportFileProvided = !StringUtils.isNullOrEmpty(reportFile);

        // we are trying to create the report at the working directory
        String workingDir = findWorkingDirPath(build);
        if (workingDir == null){
            // if we can't resolve the working directory, we will create the report at the temp directory
            workingDir = System.getProperty("java.io.tmpdir");
        }

        if (!isReportFileProvided) {
            reportFile = createReportFile(build.getResult(), workingDir, logger);
        }

        logger.info("Report file location: '" + reportFile + "'");
        return reportFile;
    }

    private String findWorkingDirPath(AbstractBuild<?, ?> build) {
        FilePath ws = build.getWorkspace();
        if (ws == null) {
            return null;
        }
        return ws.getRemote();
    }

    private String createReportFile(final Result result, String workingDir, Logger logger) {
        Map reportMap = createReportMap(result);
        return saveReportToFS(reportMap, workingDir, logger);
    }

    private Map createReportMap(Result result) {
        String status = toSealightsBuildStatus(result);
        String title = "CI Status";
        String fieldName = "status";
        String type = "string";
        String value = result.toString();

        List<Map> data = new ArrayList<>();
        Map index0 = new HashMap();
        index0.put("fieldName", fieldName);
        index0.put("type", type);
        index0.put("value", value);
        data.add(index0);

        Map<String, Object> externalReport = new HashMap<>();
        externalReport.put("title", title);
        externalReport.put("data", data);
        externalReport.put("status", status);

        return externalReport;
    }

    private String toSealightsBuildStatus(Result result) {

        // possible statuses SUCCESS, UNSTABLE, FAILURE, NOT_BUILT, ABORTED
        if (Result.SUCCESS == result) {
            return SealightsBuildStatus.SUCCESS.getName();
        } else if (Result.UNSTABLE == result || Result.FAILURE == result || Result.ABORTED == result) {
            return SealightsBuildStatus.FAILURE.getName();
        } else {
            // return empty status for every other result ('null' || NOT_BUILT)
            return null;
        }
    }

    private String saveReportToFS(Map reportMap, String workingDir, Logger logger) {
        try {
            String fileName = PathUtils.join(workingDir, "buildStatusReport_" + UUID.randomUUID() + ".json");
            logger.info("Try to create report file at '" + fileName + "'");
            File reportFile = new File(fileName);
            if (!reportFile.createNewFile()) {
                throw new RuntimeException("Failed to create new file at '" + fileName + "' for the report.");
            }
            JsonSerializer.serializeToFile(reportFile, reportMap);

            reportFile.deleteOnExit();
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create report file ", e);
        }
    }

    private BaseCommandArguments createBaseCommandArguments(
            EnvVars envVars, Properties additionalProps, ExternalReportArguments externalReportArguments, Logger logger) {

        BaseCommandArguments baseCommandArguments = new BaseCommandArguments();

        TokenData tokenData = createTokenData(externalReportArguments.getToken(), logger);
        baseCommandArguments.setTokenData(tokenData);

        baseCommandArguments.setProxy(externalReportArguments.getProxy());
        baseCommandArguments.setAppName(externalReportArguments.getAppName());
        baseCommandArguments.setBuildName(externalReportArguments.getBuildName());
        baseCommandArguments.setBranchName(externalReportArguments.getBranchName());

        baseCommandArguments.setAgentPath(resolveEnvVar(envVars, (String) additionalProps.get("agentpath")));
        baseCommandArguments.setJavaPath(resolveEnvVar(envVars, (String) additionalProps.get("javapath")));

        CommandMode commandMode = new CommandMode.ExternalReportView();
        baseCommandArguments.setMode(commandMode);

        return baseCommandArguments;
    }

    private ExternalReportArguments createExternalReportArguments(
            EnvVars envVars, AbstractBuild<?, ?> build, Properties additionalProps, String reportFile, Logger logger) {
        ExternalReportArguments externalReportArguments = new ExternalReportArguments();

        String globalToken = beginAnalysis.getDescriptor().getToken();
        externalReportArguments.setToken(resolveGlobalArgument(envVars, additionalProps, "token", globalToken));
        externalReportArguments.setTokenFile(resolveEnvVar(envVars, (String) additionalProps.get("tokenFile")));

        String globalProxy = beginAnalysis.getDescriptor().getProxy();
        externalReportArguments.setProxy(resolveGlobalArgument(envVars, additionalProps, "proxy", globalProxy));

        externalReportArguments.setBuildSessionId(resolveEnvVar(envVars, buildSessionId));
        externalReportArguments.setBuildSessionIdFile(resolveEnvVar(envVars, (String) additionalProps.get("buildSessionIdFile")));

        externalReportArguments.setAppName(resolveEnvVar(envVars, appName));

        BuildNameResolver buildNameResolver = new BuildNameResolver();
        externalReportArguments.setBuildName(buildNameResolver.getFinalBuildName(build, envVars, buildName, logger));

        externalReportArguments.setBranchName(resolveEnvVar(envVars, branchName));

        externalReportArguments.setReport(reportFile);

        return externalReportArguments;
    }

    private String resolveGlobalArgument(EnvVars envVars, Properties additionalProps, String name, String globalValue) {
        String global = JenkinsUtils.tryGetEnvVariable(envVars, (String) additionalProps.get(name));
        if (StringUtils.isNullOrEmpty(global)) {
            global = globalValue;
        }
        return global;
    }

    private String resolveFilesStorage(Properties additionalProps, EnvVars envVars) {
        String filesStorage = (String) additionalProps.get("filesstorage");
        if (!StringUtils.isNullOrEmpty(filesStorage)) {
            return resolveEnvVar(envVars, filesStorage);
        }

        filesStorage = this.beginAnalysis.getDescriptor().getFilesStorage();
        if (!StringUtils.isNullOrEmpty(filesStorage)) {
            return filesStorage;
        }

        return System.getProperty("java.io.tmpdir");
    }

    private String resolveEnvVar(EnvVars envVars, String envVarKey) {
        return JenkinsUtils.tryGetEnvVariable(envVars, envVarKey);
    }

    private TokenData createTokenData(String token, Logger logger) {
        TokenData tokenData;
        try {
            tokenData = TokenData.parse(token);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid token. Error: ", e);
        }

        TokenValidator tokenValidator = new TokenValidator();
        List<ValidationError> validationErrors = tokenValidator.validate(tokenData);
        if (validationErrors.size() > 0) {
            logger.error("Invalid token. The token contains the following errors:");
            for (ValidationError validationError : validationErrors) {
                logger.error("Field: '" + validationError.getName() + "', Error: '" + validationError.getProblem() + "'.");
            }
            throw new RuntimeException("Invalid token.");
        }

        return tokenData;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        //This is here to ensure that the reported build status is actually correct. If we were to return false here,
        //other build plugins could still modify the build result, making the sent out HipChat notification incorrect.
        return true;
    }

    @Exported
    public boolean isEnabled() {
        return enabled;
    }

    @Exported
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Exported
    public String getBuildSessionId() {
        return buildSessionId;
    }

    @Exported
    public void setBuildSessionId(String buildSessionId) {
        this.buildSessionId = buildSessionId;
    }

    @Exported
    public String getAppName() {
        return appName;
    }

    @Exported
    public void setAppName(String appName) {
        this.appName = appName;
    }

    @Exported
    public String getBranchName() {
        return branchName;
    }

    @Exported
    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    @Exported
    public CommandBuildName getBuildName() {
        return buildName;
    }

    @Exported
    public void setBuildName(CommandBuildName buildName) {
        this.buildName = buildName;
    }

    @Exported
    public String getAdditionalArguments() {
        return additionalArguments;
    }

    @Exported
    public void setAdditionalArguments(String additionalArguments) {
        this.additionalArguments = additionalArguments;
    }

    @Exported
    public BeginAnalysis getBeginAnalysis() {
        return beginAnalysis;
    }

    @Exported
    public void setBeginAnalysis(BeginAnalysis beginAnalysis) {
        this.beginAnalysis = beginAnalysis;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public String getDisplayName() {
            return "SeaLights - Report Build Status";
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public DescriptorExtensionList<CommandBuildName, CommandBuildName.CommandBuildNameDescriptor> getBuildNameDescriptorList() {
            return Jenkins.getInstance().getDescriptorList(CommandBuildName.class);
        }

    }
}