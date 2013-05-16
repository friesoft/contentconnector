package com.gentics.cr.util.indexing;

import com.gentics.cr.CRConfig;

/**
 * Task, that is executed after the index-job has finished.
 * @author voglerc
 */
public interface IAfterActionTask {

	/**
	 * Executes the task.
	 * @param config configuration
	 */
	void execute(final CRConfig config);

}
