package com.gentics.cr.util.indexing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gentics.cr.configuration.GenericConfiguration;

/**
 * Abstract implementation of an IActionTask.
 * 
 * @author voglerc
 */
public abstract class AbstractAfterActionTask implements IAfterActionTask {

	/**
	 * key for configuration.
	 */
	private static final String AFTERACTION_KEY = "afteractiontask";

	/**
	 * class that will be executed.
	 */
	private static final String AFTERACTION_CLASS_KEY = "class";

	/**
	 * Logging.
	 */
	protected static final Logger LOGGER = Logger.getLogger(AbstractAfterActionTask.class);

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
						t = (IAfterActionTask) Class.forName(taskClass).getConstructor(((Class<?>) null)).newInstance();
						if (t != null) {
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

}
