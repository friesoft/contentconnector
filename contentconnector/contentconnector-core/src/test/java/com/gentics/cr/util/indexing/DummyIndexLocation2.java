package com.gentics.cr.util.indexing;

import java.util.concurrent.ConcurrentHashMap;

import com.gentics.cr.CRConfig;
import com.gentics.cr.CRConfigUtil;
import com.gentics.cr.exceptions.CRException;

/**
 * DummyIndexLocation for Testing.
 * 
 * @author patrickhoefer
 */
public class DummyIndexLocation2 extends IndexLocation {

	/**
	 * @param givenConfig configuration for DummyIndexLocation
	 */
	protected DummyIndexLocation2(final CRConfig givenConfig) {
		super(givenConfig);
	}

	@Override
	public void createReopenFile() {
		// do nothing
	}

	@Override
	public void checkLock() throws Exception {
		// do nothing
	}

	@Override
	public boolean isLocked() {
		return false;
	}

	@Override
	protected void finalize() {
		// do nothing
	}

	/**
	 * Overrides createCRIndexJob and print out which index Job was created. Is used in CreateAllCRIndexJobsOrderTest.
	 * 
	 * @param config configuration
	 * @param configmap configurationmap
	 * @return <code>null</code>
	 */
	@Override
	public AbstractUpdateCheckerJob createIndexJobInstance(final CRConfig config, final ConcurrentHashMap<String, CRConfigUtil> configmap) {
		System.out.print("Create Job: " + config.getName() + " ");
		return new AbstractUpdateCheckerJob(config, this, configmap) {

			@Override
			protected void indexCR(final IndexLocation indexLocation, final CRConfigUtil config) throws CRException {
				// do nothing
			}
		};
	}

}
