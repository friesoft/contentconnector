package com.gentics.cr.util.indexing;

import com.gentics.cr.CRConfig;

public interface IAfterActionTask {

	void execute(CRConfig config);

	void setActionkey(String key);

}
