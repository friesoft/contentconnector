package com.gentics.cr;

import java.io.File;
import java.net.URISyntaxException;

import com.gentics.DefaultTestConfiguration;
import com.gentics.cr.configuration.EnvironmentConfiguration;
import com.gentics.cr.plink.PlinkProcessor;

public class HSQLTestConfigFactory {
	
	private static final String CR_1_PREFIX = "RP.1.";
	
	/**
	 * This method generates a DB configuration for an in memory HSQL.
	 * A default empty repository will be generated.
	 * Default name: mytestdatasource
	 * @return
	 * @throws URISyntaxException 
	 */
	public static final CRConfigUtil getDefaultHSQLConfiguration(Class testClazz) throws URISyntaxException {
		return getDefaultHSQLConfiguration("mytestdatasource" + testClazz.toString());
	}
	
	/**
	 * This method generates a DB configuration for an in memory HSQL.
	 * A default empty repository will be generated.
	 * @param dsName name of the datasource
	 * @param cache true if cache should be enabled (default = false)
	 * @return
	 * @throws URISyntaxException 
	 */
	public static final CRConfigUtil getDefaultHSQLConfiguration(String dsName, boolean cache) throws URISyntaxException {
		EnvironmentConfiguration.setConfigPath(new File(DefaultTestConfiguration.class.getResource("conf/gentics").getFile()).getAbsolutePath());
		EnvironmentConfiguration.loadEnvironmentProperties();

		CRConfigUtil config = new CRConfigUtil();
		config.setName(dsName);
		if (!cache) {
			config.set(CR_1_PREFIX + RequestProcessor.CONTENTCACHE_KEY, "false");
			config.set(CR_1_PREFIX + PlinkProcessor.PLINK_CACHE_ACTIVATION_KEY, "false");
		}
		config.set(CR_1_PREFIX + "ds-handle.url", "jdbc:hsqldb:mem:" + dsName);
		config.set(CR_1_PREFIX + "ds-handle.driverClass", "org.hsqldb.jdbcDriver");
		config.set(CR_1_PREFIX + "ds-handle.shutDownCommand", "SHUTDOWN");
		config.set(CR_1_PREFIX + "ds-handle.type", "jdbc");
		config.set(CR_1_PREFIX + "ds.sanitycheck2", "true");
		config.set(CR_1_PREFIX + "ds.autorepair2", "true");
		config.set(CR_1_PREFIX + "ds.sanitycheck", "false");
		return config;
	}
	
	/**
	 * This method generates a DB configuration for an in memory HSQL.
	 * A default empty repository will be generated.
	 * @param dsName name of the datasource
	 * @return
	 * @throws URISyntaxException 
	 */
	public static final CRConfigUtil getDefaultHSQLConfiguration(String dsName) throws URISyntaxException {
		return getDefaultHSQLConfiguration(dsName, false);
	}

}
