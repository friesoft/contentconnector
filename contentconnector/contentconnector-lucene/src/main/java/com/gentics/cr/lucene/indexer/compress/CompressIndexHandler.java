package com.gentics.cr.lucene.indexer.compress;

import com.gentics.cr.events.AbstractIndexNameAwareEventReceiver;
import com.gentics.cr.events.Event;
import com.gentics.cr.events.JobQueueFinishedEvent;

/**
 * Extension, that runs after the completition of the index-job-queue and creates an archive of the index.
 * @author voglerc
 *
 */
public class CompressIndexHandler extends AbstractIndexNameAwareEventReceiver {

	@Override
	public void processEvent(final Event<?> event) {
		if (!JobQueueFinishedEvent.JOBQUEUE_FINISHED_EVENT_TYPE.equals(event.getType())) {
			// if eventtype doesn't match => do nothing
			return;
		}
		JobQueueFinishedEvent jobQueueFinishedEvent = (JobQueueFinishedEvent) event;
		if (!jobQueueFinishedEvent.getData().equals(indexName)) {
			// if indexname doesn't match => do nothing
			return;
		}

		IndexCompressor indexCompressor = new IndexCompressor();
		// get indexname from event
		indexCompressor.compress(jobQueueFinishedEvent.getData());
	}
}
