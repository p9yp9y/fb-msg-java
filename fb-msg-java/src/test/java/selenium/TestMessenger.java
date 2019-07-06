package selenium;

import static java.lang.System.getProperty;

import org.junit.Test;

public class TestMessenger {
	
	@Test
	public void testConnect() throws InterruptedException {
		Messenger messenger = new Messenger();

		messenger.login(getProperty("email"), getProperty("password"));
		
		messenger.startConversation("Piroska Ráchegyi");
		
		messenger.send("Test message 1");
		
		messenger.waitBrowser();
		
		//messenger.logout();
		
		//messenger.closeBrowser();
	}
}