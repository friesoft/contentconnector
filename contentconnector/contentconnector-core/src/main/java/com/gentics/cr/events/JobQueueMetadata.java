package com.gentics.cr.events;

import org.apache.commons.lang.StringUtils;

/**
 * Metadata for a jobqueue.
 * @author voglerc
 */
public class JobQueueMetadata {

	/**
	 * index name.
	 */
	private String index;

	/**
	 * was the index modified?
	 */
	private boolean modifiedIndex;

	/**
	 * @return the index name
	 */
	public String getIndex() {
		return index;
	}

	/**
	 * Set the indexname (also supporting identifiers from cr-config).
	 * @param index indexname
	 */
	public void setIndex(final String index) {
		if (!StringUtils.isEmpty(index) && index.startsWith("index")) {
			this.index = StringUtils.split(index, ".")[1];
		} else {
			this.index = index;
		}
	}

	/**
	 * @return <code>true</code> if one job in the jobqueue modified the index, otherwise <code>false</code>
	 */
	public boolean isModifiedIndex() {
		return modifiedIndex;
	}

	/**
	 * set the <code>modifiedIndex</code>-Flag.
	 * @param modifiedIndex if the index was modified
	 */
	public void setModifiedIndex(final boolean modifiedIndex) {
		this.modifiedIndex = modifiedIndex;
	}

}
