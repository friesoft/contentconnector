package com.gentics.cr.util.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gentics.cr.configuration.GenericConfiguration;

public abstract class AbstractAfterActionTask implements IAfterActionTask {

	private static final String AFTERACTION_KEY = "afteractiontask";
	private static final String AFTERACTION_CLASS_KEY = "class";

	protected static final Logger LOGGER = Logger.getLogger(AbstractAfterActionTask.class);

	protected String actionKey;

	/**
	 * @param config - configuration containing the definition of the transformers
	 * @return List of ContentTransformer defined in the confguration.
	 */
	public static List<IAfterActionTask> getTaskList(final GenericConfiguration config) {
		GenericConfiguration tconf = (GenericConfiguration) config.get(AFTERACTION_KEY);
		if (tconf != null) {
			Map<String, GenericConfiguration> confs = tconf.getSortedSubconfigs();
			if (confs != null && confs.size() > 0) {
				List<IAfterActionTask> ret = new ArrayList<IAfterActionTask>(confs.size());
				for (Map.Entry<String, GenericConfiguration> e : confs.entrySet()) {
					GenericConfiguration c = e.getValue();
					String taskClass = (String) c.get(AFTERACTION_CLASS_KEY);
					try {
						IAfterActionTask t = null;
						t = (IAfterActionTask) Class.forName(taskClass).getConstructor(null).newInstance();
						if (t != null) {
							t.setActionkey(e.getKey());
							ret.add(t);
						}
					} catch (Exception ex) {
						LOGGER.error("Invalid configuration found. Could not instantiate " + taskClass, ex);
					}

				}
				return (ret);
			}
		}

		return null;
	}

	public void setActionkey(final String key) {
		this.actionKey = key;
	}

}
