package selenium;

import static java.lang.System.getProperty;

import org.junit.Test;

public class TestMessenger {
	
	@Test
	public void testConnect() {
		Messenger messenger = new Messenger();

		messenger.connect(getProperty("email"), getProperty("password"));
	}
}
