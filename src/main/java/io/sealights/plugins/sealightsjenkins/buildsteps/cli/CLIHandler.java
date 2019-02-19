package io.sealights.plugins.sealightsjenkins.buildsteps.cli;

import io.sealights.plugins.sealightsjenkins.buildsteps.cli.entities.AbstractCommandArgument;
import io.sealights.plugins.sealightsjenkins.buildsteps.cli.entities.BaseCommandArguments;
import io.sealights.plugins.sealightsjenkins.buildsteps.cli.entities.CommandModes;
import io.sealights.plugins.sealightsjenkins.buildsteps.cli.executors.CommandExecutorsFactory;
import io.sealights.plugins.sealightsjenkins.buildsteps.cli.executors.ICommandExecutor;
import io.sealights.plugins.sealightsjenkins.entities.TokenData;
import io.sealights.plugins.sealightsjenkins.integration.upgrade.AbstractUpgradeManager;
import io.sealights.plugins.sealightsjenkins.integration.upgrade.BuildScannerUpgradeManager;
import io.sealights.plugins.sealightsjenkins.integration.upgrade.TestListenerUpgradeManager;
import io.sealights.plugins.sealightsjenkins.integration.upgrade.UpgradeProxy;
import io.sealights.plugins.sealightsjenkins.integration.upgrade.entities.UpgradeConfiguration;
import io.sealights.plugins.sealightsjenkins.utils.Logger;
import io.sealights.plugins.sealightsjenkins.utils.StringUtils;

import java.io.File;

/**
 * This class is responsible to invoke the right command executor.
 */
public class CLIHandler {

    private Logger logger;
    private String filesStorage;
    private BaseCommandArguments baseArgs;
    private AbstractCommandArgument commandArgument;

    public CLIHandler(Logger logger) {
        this.logger = logger;
    }

    public CLIHandler(BaseCommandArguments baseArgs, AbstractCommandArgument commandArgument, String filesStorage, Logger logger) {
        this.baseArgs = baseArgs;
        this.commandArgument = commandArgument;
        this.filesStorage = filesStorage;
        this.logger = logger;
    }

    public boolean handle() {
        logger.info("Base Arguments Provided for the cli execution: " + baseArgs.toString());

        String agentPath = tryGetAgentPath(logger, baseArgs);
        baseArgs.setAgentPath(agentPath);

        CommandExecutorsFactory commandExecutorsFactory = new CommandExecutorsFactory();
        ICommandExecutor executor = commandExecutorsFactory.createExecutor(logger, baseArgs, commandArgument);

        return executor.execute();
    }

    private String tryGetAgentPath(Logger logger, BaseCommandArguments baseArgs) {
        if (!StringUtils.isNullOrEmpty(baseArgs.getAgentPath()) && new File(baseArgs.getAgentPath()).isFile()) {
            return baseArgs.getAgentPath();
        }
        AbstractUpgradeManager upgradeManager = createUpgradeManager(logger, baseArgs);
        return upgradeManager.ensureLatestAgentPresentLocally();
    }

    private AbstractUpgradeManager createUpgradeManager(Logger logger, BaseCommandArguments baseArgs) {
        UpgradeConfiguration upgradeConfiguration = createUpgradeConfiguration(baseArgs);
        UpgradeProxy upgradeProxy = new UpgradeProxy(upgradeConfiguration, logger);
        return getRelevantUpgradeManager(upgradeProxy, upgradeConfiguration);
    }

    private UpgradeConfiguration createUpgradeConfiguration(BaseCommandArguments baseArgs) {

        String token = null;
        String customerId = baseArgs.getCustomerId();
        String server = baseArgs.getUrl();

        TokenData tokenData = baseArgs.getTokenData();
        if (tokenData != null){
            token = tokenData.getToken();
            customerId = tokenData.getCustomerId();
            server = tokenData.getServer();
        }

        return new UpgradeConfiguration(
                token,
                customerId,
                baseArgs.getBuildSessionId(),
                baseArgs.getAppName(),
                baseArgs.getLabId(),
                baseArgs.getBranchName(),
                server,
                baseArgs.getProxy(),
                filesStorage
        );
    }

    public String getFilesStorage() {
        return filesStorage;
    }

    public void setFilesStorage(String filesStorage) {
        this.filesStorage = filesStorage;
    }

    public BaseCommandArguments getBaseArgs() {
        return baseArgs;
    }

    public void setBaseArgs(BaseCommandArguments baseArgs) {
        this.baseArgs = baseArgs;
    }

    public void setCommandArgument(AbstractCommandArgument commandArgument) {
        this.commandArgument = commandArgument;
    }

    private AbstractUpgradeManager getRelevantUpgradeManager(
            UpgradeProxy upgradeProxy, UpgradeConfiguration upgradeConfiguration) {

        boolean useScannerUpgradeManager =
         CommandModes.Config.equals(commandArgument.getMode()) || CommandModes.PrConfig.equals(commandArgument.getMode());

        if (useScannerUpgradeManager) {
            return new BuildScannerUpgradeManager(upgradeProxy, upgradeConfiguration, logger);
        }
        return new TestListenerUpgradeManager(upgradeProxy, upgradeConfiguration, logger);
    }
}

