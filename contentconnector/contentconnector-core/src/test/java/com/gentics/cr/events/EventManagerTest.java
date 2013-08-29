package com.gentics.cr.events;

import static org.junit.Assert.fail;

import org.junit.Test;

public class EventManagerTest {

	final static class DummyEvent extends Event<String> {
		@Override
		public String getType() {
			return null;
		}
		@Override
		public String getData() {
			return null;
		}
	}

	static boolean ran = false;
	static Thread t = new Thread(new Runnable() {
		@Override
		public void run() {
			System.out.println("run()");
			EventManager.getInstance().fireEvent(new DummyEvent());
			System.out.println("end run()");
		}
	});

	private static class Rec implements IEventReceiver {
		@Override
		public void processEvent(final Event<?> event) {
			if (ran) {
				// no deadlock!
			} else if (t.isAlive()) {
				fail("Thread already alive.");
			} else {
				ran = true;
				System.out.println("t.start()");
				t.start();
				try {
					t.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("end method");
			}
		}

	}

	@Test(timeout = 1000)
	public void testDeadLock() {

		System.out.println(t.isAlive());
		EventManager.getInstance().register(new Rec());
		EventManager.getInstance().fireEvent(new DummyEvent());

		//		try {
		//			t.join();
		//			System.out.println("done");
		//		} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}
	}

}
