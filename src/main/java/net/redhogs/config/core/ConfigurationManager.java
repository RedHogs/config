package net.redhogs.config.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Controls the configuration. The default configuration consists of a master configuration that's shipped and another override configuration file that's extracted or supplied by the user.
 * 
 * @author grhodes
 * @since 14 Mar 2013 13:13:28
 */
public final class ConfigurationManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class);
    private static final CompositeConfiguration config = new CompositeConfiguration();
    private static XMLConfiguration overrideConfig;

    private ConfigurationManager() {
    }

    /**
     * Loads the configuration according to the following logic.
     * <ol>
     * <li>If the -Dconfig system property has not been set we use a supplied directory in the temporary directory.</li>
     * <li>If we find a previous configuration file in the relative directory, we'll use that, otherwise, extract the default packaged config file.</li>
     * <li>The configuration extracted or found will then be loaded and cached in the system.</li>
     * </ol>
     */
    public static synchronized void load(String defaultDirectory) {
        File configFile = getConfigFile(getConfigDirectory(defaultDirectory));
        try {
            overrideConfig = new XMLConfiguration(configFile);
            config.addConfiguration(overrideConfig, true);
            addMasterConfig(); // Defaults must be added last
            LOG.info("Configuration [{}] successfully loaded.", configFile.getAbsolutePath());
        } catch (ConfigurationException cex) {
            LOG.error("Exception reading configuration.", cex);
        }
    }

    /**
     * Saves the configuration to the override configuration file.
     */
    public static synchronized void save() {
        if (overrideConfig != null) {
            try {
                overrideConfig.save();
            } catch (ConfigurationException e) {
                LOG.error("Exception saving configuration.", e);
            }
        }
    }

    /**
     * @return
     */
    private static void addMasterConfig() {
        InputStream inputStream = null;
        try {
            inputStream = ConfigurationManager.class.getResourceAsStream(ConfigurationManagerConstants.MASTER_CONFIG_FILENAME);
            final XMLConfiguration masterXmlConfig = new XMLConfiguration();
            masterXmlConfig.load(inputStream);
            config.addConfiguration(masterXmlConfig);
        } catch (ConfigurationException cex) {
            LOG.error("Exception reading master configuration.", cex);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     * If we find a previous configuration file in the configDirectory, we'll use that, otherwise, extract the default packaged config file.
     * 
     * @param configDirectory
     * @return
     */
    private static File getConfigFile(File configDirectory) {
        File configFile = new File(configDirectory, ConfigurationManagerConstants.CONFIG_FILENAME);
        if (!configFile.exists()) {
            LOG.info("Configuration not found, using default: {}.", configFile.getAbsolutePath());
            InputStream inputStream = null;
            try {
                configFile.createNewFile();
                inputStream = ConfigurationManager.class.getResourceAsStream(ConfigurationManagerConstants.CONFIG_FILENAME);
                FileUtils.copyInputStreamToFile(inputStream, configFile);
            } catch (IOException e) {
                LOG.error("Exception extracting default configuration.", e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return configFile;
    }

    /**
     * If the -Dconfig system property has not been set we use a supplied directory in the temporary directory, otherwise return the directory specified.
     * 
     * @return
     */
    private static File getConfigDirectory(String defaultDirectory) {
        File configDirectory;
        String directorySetting = System.getProperty(ConfigurationManagerConstants.CONFIG_PROPERTY);
        if (StringUtils.isEmpty(directorySetting)) {
            configDirectory = new File(FileUtils.getTempDirectoryPath(), defaultDirectory);
            LOG.info("Config directory not specified, using default: {}.", configDirectory.getAbsolutePath());
        } else {
            configDirectory = new File(directorySetting);
        }
        if (!configDirectory.exists()) {
            LOG.info("Config directory {} did not already exist. Creating...", configDirectory.getAbsolutePath());
            configDirectory.mkdirs();
        }
        return configDirectory;
    }

    /**
     * @return the config
     */
    public static CompositeConfiguration getConfig() {
        return config;
    }

}
