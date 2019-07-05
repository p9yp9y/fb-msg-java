package selenium;

import static java.lang.System.getProperty;

import org.junit.Test;

public class TestMessenger {
	
	@Test
	public void testConnect() throws InterruptedException {
		Messenger messenger = new Messenger();

		messenger.connect(getProperty("email"), getProperty("password"));
		
		messenger.selectFriend("Piroska Ráchegyi");
		
		messenger.send("Test message");
		
		//messenger.waitBrowser();
		messenger.closeBrowser();
	}
}
