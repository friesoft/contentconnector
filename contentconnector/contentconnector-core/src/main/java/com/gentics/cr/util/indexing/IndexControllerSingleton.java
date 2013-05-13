package com.gentics.cr.util.indexing;

import com.gentics.cr.util.indexing.IndexController;

/**
 * We need this helper class to prevent the webservice IndexControllerService from
 * instantiating the IndexController on each call. (MUCH to expensive leading to OutOfMemory)
 *  
 * @author Friedreich Bernhard
 */
public final class IndexControllerSingleton {

	/** Prevent use as this class should only serve an instance of IndexController. */
	private IndexControllerSingleton() {
	}

	/**
	 * Instance of IndexController.
	 */
	private static IndexController indexer = null;
	
	/**
	 * Used to retreive one IndexController Instance.
	 * @return on first call a new IndexController is initialized, afterwards on each call
	 * the previously created instance is returned. Hopefully IndexController is threadsave??.
	 */
	public static IndexController getIndexControllerInstance() {
		if (indexer == null) {
			indexer = IndexController.get("indexer");
		}
		return indexer;
	}
}
