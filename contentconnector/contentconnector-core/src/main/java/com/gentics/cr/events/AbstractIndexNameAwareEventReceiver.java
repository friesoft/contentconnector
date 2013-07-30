package com.gentics.cr.events;

import org.apache.commons.lang.StringUtils;

/**
 * General Interface for Content Connector Event Receivers which need access to the index name.
 * @author voglerc
 *
 */
public abstract class AbstractIndexNameAwareEventReceiver implements IEventReceiver {

	/**
	 * indexname.
	 */
	protected String indexName;

	/**
	 * Set the indexname (also supporting identifiers from cr-config).
	 * @param indexName indexname
	 */
	public void setIndexName(final String indexName) {
		if (!StringUtils.isEmpty(indexName) && indexName.startsWith("index")) {
			this.indexName = StringUtils.split(indexName, ".")[1];
		} else {
			this.indexName = indexName;
		}
	}

}
