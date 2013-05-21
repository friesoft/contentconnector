package com.gentics.cr.events;

import org.apache.commons.lang.StringUtils;

/**
 * Event after the job-queue has been finished.
 * @author voglerc
 *
 */
public class JobQueueFinishedEvent extends Event<String> {

	/**
	 * unique name.
	 */
	public static final String JOBQUEUE_FINISHED_EVENT_TYPE = "JOBQUEUEFINISHEDEVENT";

	/**
	 * index name.
	 */
	private String index;

	/**
	 * Creates a new event with the given identifyer.
	 * @param identifyer identifyer
	 */
	public JobQueueFinishedEvent(final String identifyer) {
		if (!StringUtils.isEmpty(identifyer) && identifyer.startsWith("index")) {
			index = StringUtils.split(identifyer, ".")[1];
		} else {
			index = identifyer;
		}
	}

	@Override
	public String getData() {
		return this.index;
	}

	@Override
	public String getType() {
		return JOBQUEUE_FINISHED_EVENT_TYPE;
	}

}
