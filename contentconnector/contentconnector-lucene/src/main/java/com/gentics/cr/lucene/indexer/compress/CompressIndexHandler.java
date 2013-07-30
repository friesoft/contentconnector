package com.gentics.cr.lucene.indexer.compress;

import com.gentics.cr.events.AbstractIndexNameAwareEventReceiver;
import com.gentics.cr.events.Event;
import com.gentics.cr.events.JobQueueFinishedEvent;
import com.gentics.cr.events.JobQueueMetadata;

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
		JobQueueMetadata jobQueueMetadata = jobQueueFinishedEvent.getData();
		if (!jobQueueMetadata.getIndex().equals(indexName) || !jobQueueMetadata.isModifiedIndex()) {
			// if indexname doesn't match or the index wasn't modified => do nothing
			return;
		}

		new IndexCompressor(jobQueueMetadata.getIndex()).start();
	}
}
