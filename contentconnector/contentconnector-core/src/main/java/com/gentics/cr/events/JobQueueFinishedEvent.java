package com.gentics.cr.events;

/**
 * Event after the job-queue has been finished.
 * @author voglerc
 *
 */
public class JobQueueFinishedEvent extends Event<JobQueueMetadata> {

	/**
	 * unique name.
	 */
	public static final String JOBQUEUE_FINISHED_EVENT_TYPE = "JOBQUEUEFINISHEDEVENT";

	/**
	 * jobqueue-metadata. 
	 */
	private JobQueueMetadata jobQueueMetadata;

	/**
	 * Creates a new event with the given identifyer.
	 * @param jobQueueMetadata metadata 
	 */
	public JobQueueFinishedEvent(final JobQueueMetadata jobQueueMetadata) {
		this.jobQueueMetadata = jobQueueMetadata;
	}

	@Override
	public JobQueueMetadata getData() {
		return this.jobQueueMetadata;
	}

	@Override
	public String getType() {
		return JOBQUEUE_FINISHED_EVENT_TYPE;
	}

}
