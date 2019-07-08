package selenium;

import static java.lang.System.getProperty;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.github.p9yp9y.messenger.Messenger;

public class TestMessenger {
	private final static Logger LOGGER = Logger.getLogger(Messenger.class.getName());

	@Test
	public void testMessenger() throws InterruptedException {
		Messenger messenger = new Messenger() //
				.addMessageListener(message -> {
					LOGGER.log(Level.INFO, message.toString());
				}) //
				.login(getProperty("email"), getProperty("password")) //
				.startConversation("András Dobrosi") //
				.send("Test message 1");
		messenger.waitBrowser();

		// messenger.logout();

		// messenger.closeBrowser();
	}

	@Test
	public void testMessengerAlive() throws InterruptedException, IOException {
		new Messenger().alive(getProperty("email"), getProperty("password"), "András Dobrosi", System.out, System.in);
	}
}
