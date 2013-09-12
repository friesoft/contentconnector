package com.gentics.cr.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.cr.CRResolvableBean;

public class CRResolvableBeanTest {

	@Test
	public void testContentidConstructor() {
		CRResolvableBean bean = new CRResolvableBean("3.4");
		assertEquals("Contentid not correct.", "3.4", bean.getContentid());
		assertEquals("obj_id not correct.", "4", bean.getObj_id());
		assertEquals("obj_type not correct.", "3", bean.getObj_type());
		assertEquals("obj_id via default get not correct.", "4", bean.get("obj_id"));
		assertEquals("obj_type via default get not correct.", "3", bean.get("obj_type"));
	}

	@Test
	public void testFormat() {
		CRResolvableBean bean = new CRResolvableBean("10007.8");
		bean.set("name", "Testname");
		assertEquals("10007.8", bean.format());

		assertEquals("Testname: 10007.8", bean.format("%(name): %(contentid)"));

		assertEquals("null: 10007.8", bean.format("%(asdf): %(contentid)"));

		bean.set("myId", "10002");
		assertEquals("10002: Testname", bean.format("myId", "%(idAttribute): %(name)"));

		CRResolvableBean bean2 = new CRResolvableBean();
		assertEquals("10001", bean2.format());
	}

}
