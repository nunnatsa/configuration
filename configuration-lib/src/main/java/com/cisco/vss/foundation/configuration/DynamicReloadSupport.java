/**
 *
 */
package com.cisco.vss.foundation.configuration;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class reads the configuration to determine if dynamic reload of the configuration in case of config file changes should be enabled or not.<br>
 * When enabled the configuration in memory map will be updated upon file changes within a configuration refresh delay period.<br>
 * Client interested in getting notifications of configuration reload should register via {@link CabConfigurationListenerRegistry#addCabConfigurationListener(CabConfigurationListener)} API.
 * 
 * @Deprecated
 * @author Yair Ogen
 */
@Deprecated
public class DynamicReloadSupport {

	private static final Logger LOGGER = Logger.getLogger(DynamicReloadSupport.class);

	private final CompositeConfiguration configuration;

	private DynamicReloadSupport(CompositeConfiguration configuration) {
		super();
		this.configuration = configuration;
	}

	public void init() {

		LOGGER.debug("in DynamicReloadSupport init method");

		boolean isDynamicReLoadEnabled = configuration.getBoolean("configuration.dynamicConfigReload.enabled", false);
		boolean isDynamicReloadAutoUpdateEnabled = true;//configuration.getBoolean("configuration.dynamicConfigReload.autoUpdateEnabled");
		long refreshDelay = configuration.getLong("configuration.dynamicConfigReload.refreshDelay", 30000);

		if (isDynamicReLoadEnabled) {

			LOGGER.info("configuration dynamic reload is enabled!");

			int numberOfConfigurations = configuration.getNumberOfConfigurations();

			for (int index = 0; index < numberOfConfigurations; index++) {

				Configuration config = configuration.getConfiguration(index);

				// file reload only supported on file based configurations.
				// cab configuration only supports properties configuration.
				if (config instanceof PropertiesConfiguration) {

					PropertiesConfiguration propertiesConfiguration = (PropertiesConfiguration) config;

					// TODO: ignore default config files
					String fileName = propertiesConfiguration.getFileName();
					if (fileName == null) {
						fileName = propertiesConfiguration.getBasePath();
					}
					if (fileName.startsWith("default")) {
						// do not support reload for default config files.
						continue;
					}

					LOGGER.debug("Setting reload strategy on: " + propertiesConfiguration.getPath());

					CABFileChangedReloadingStrategy strategy = new CABFileChangedReloadingStrategy();
					strategy.setRefreshDelay(refreshDelay);

					propertiesConfiguration.setReloadingStrategy(strategy);

				}

			}

			if (isDynamicReloadAutoUpdateEnabled) {
				LOGGER.info("configuration dynamic reload automatic update is enabled!");
				startDynamicReloadAutoUpdateDeamon(configuration, refreshDelay);
			}

		}

	}

	/**
	 * @param refreshDelay
	 */
	private void startDynamicReloadAutoUpdateDeamon(final Configuration configuration, final long refreshDelay) {

		// run as daemon
		Timer timer = new Timer("DynamicReloadAutoUpdate", true);

		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				
				try {
					Field configCacheField = FoundationCompositeConfiguration.class.getDeclaredField("DISABLE_CACHE");
					configCacheField.setAccessible(true);
					Field modifiersField = Field.class.getDeclaredField("modifiers");
					modifiersField.setAccessible(true);
					modifiersField.setInt(configCacheField, configCacheField.getModifiers() & ~Modifier.FINAL);
					boolean disableCahce = (Boolean) configCacheField.get(configuration);
					if(!disableCahce){
						configCacheField.set(configuration, Boolean.TRUE);
						// just for triggering reload mechanism
						configuration.getProperty("configuration.dynamicConfigReload.enabled");
						configCacheField.set(configuration, Boolean.FALSE);
					}else{
						// just for triggering reload mechanism
						configuration.getProperty("configuration.dynamicConfigReload.enabled");
					}
				} catch (Exception e) {					
					LOGGER.error("problem reloading the configuration", e);
				}
				

			}
		}, refreshDelay, refreshDelay);
	}

}