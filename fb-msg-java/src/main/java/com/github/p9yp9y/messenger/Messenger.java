package com.github.p9yp9y.messenger;

import static java.lang.Runtime.getRuntime;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;
import com.github.p9yp9y.messenger.Message.Type;

import pgy.master.util.JCommanderUtil;

public class Messenger {
	private final static Logger LOGGER = Logger.getLogger(Messenger.class.getName());

	@Parameter(names = {"-method"}, required = true)
	private String method;

	@Parameter(names = {"-email"})
	private String email;

	@Parameter(names = {"-password"})
	private String password;

	@Parameter(names = {"-message"})
	private String message;

	@Parameter(names = {"-friendName"})
	private String friendName;

	@Parameter(names = {"-out"}, converter = OutputStreamConverter.class)
	private String out;

	@Parameter(names = {"-in"}, converter = InputStreamConverter.class)
	private InputStream in;

	private List<MessageReceiveListener> messageListeners = new ArrayList<>();

	private static final String MESSAGES_URL = "https://m.facebook.com/messages/";
	private By threadListRows = By.id("threadlist_rows");
	private ChromeDriver browser;
	private WebDriverWait wait;

	private class InputStreamConverter implements IStringConverter<InputStream> {
		@Override
		public InputStream convert(final String value) {
			InputStream res = System.in;
			try {
				res = new FileInputStream(new File(value));
			} catch (FileNotFoundException e) {
				LOGGER.log(Level.SEVERE, e.toString(), e);
			}
			return res;
		}
	}

	private class OutputStreamConverter implements IStringConverter<OutputStream> {
		@Override
		public OutputStream convert(final String value) {
			OutputStream res = System.out;
			try {
				res = new FileOutputStream(new File(value));
			} catch (FileNotFoundException e) {
				LOGGER.log(Level.SEVERE, e.toString(), e);
			}
			return res;
		}
	}

	public Messenger() {
		init();
	}

	public Messenger(final String[] args) {
		JCommanderUtil.parseArgs(this, args);
		init();
		exec();
	}

	public static void main(final String[] args) {
		new Messenger(args);
	}

	private void exec() {
		if ("login".equals(method)) {
			login(email, password);
		} else if ("logout".equals(method)) {
			logout();
		} else if ("send".equals(method)) {
			send(message);
		} else if ("startConversation".equals(method)) {
			startConversation(friendName);
		} else if ("alive".equals(method)) {
			try {
				alive(email, password, friendName, System.out, System.in);
			} catch (InterruptedException | IOException e) {
				LOGGER.log(Level.WARNING, e.toString(), e);
			}
		} else {
			throw new RuntimeException("This method not found: " + method + ". Please define an existing -method parameter.");
		}
	}

	private void init() {
		System.setProperty("webdriver.chrome.driver", "/usr/lib/chromium-browser/chromedriver");

		ChromeOptions opt = new ChromeOptions();

		String home = System.getProperty("user.home");
		File f = new File(home + "/.config/java-messenger/chromium-profile");
		f.mkdirs();

		opt.addArguments("user-data-dir=" + f.getPath());
		opt.setBinary("/usr/bin/chromium-browser");

		browser = new ChromeDriver(opt);

		getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				closeBrowser();
			}
		});

		wait = new WebDriverWait(browser, 10);
	}

	public Messenger login(final String email, final String password) {
		WebElement e;
		browser.get(MESSAGES_URL);
		try {
			e = browser.findElement(By.id("m_login_email"));
			e.clear();
			e.sendKeys(email);
		} catch (NoSuchElementException exc) {
			try {
				e = browser.findElement(By.tagName("form"));
			} catch (NoSuchElementException exc2) {
				return this;
			}
			if (e.getAttribute("action").contains("/login/device-based/validate-pin/")) {
				e.submit();
			} else {
				return this;
			}
		}

		e = browser.findElement(By.xpath("//input[@type='password']"));
		e.clear();
		e.sendKeys(password);

		e = browser.findElement(By.xpath("//button"));
		e.click();

		waitForThreadListRows();

		return this;
	}

	public void logout() {
		browser.manage().deleteAllCookies();
	}

	public List<String> listFriends() {
		return getFriendsRows().stream().map(r -> getFriendName(r)).collect(toList());
	}

	public Messenger startConversation(final String friendName) {
		try {
			browser.findElement(threadListRows);
		} catch (NoSuchElementException exc) {
			browser.get(MESSAGES_URL);
			waitForThreadListRows();
		}

		getFriendRow(friendName).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("_z3m")));

		List<WebElement> l = getMessages(browser);

		l.forEach(r -> {
			LOGGER.log(Level.INFO, createMessage(r).toString());
		});

		startListener();

		return this;
	}

	public Messenger addMessageListener(final MessageReceiveListener messageListener) {
		messageListeners.add(messageListener);
		return this;
	}

	public Messenger removeMessageListener(final MessageReceiveListener messageListener) {
		messageListeners.remove(messageListener);
		return this;
	}

	public Messenger send(final String message) {
		WebElement e = browser.findElement(By.id("composerInput"));
		e.sendKeys(message);

		e = browser.findElement(By.xpath("//button[@name='send']"));
		e.click();

		return this;
	}

	public Messenger alive(final String email, final String password, final String friendName, final OutputStream out, final InputStream in)
			throws InterruptedException, IOException {
		login(email, password).startConversation(friendName);

		startWriter(in);
		startReader(out);

		return waitBrowser();
	}

	private void startReader(final OutputStream out) {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
		addMessageListener(l -> {
			try {
				writer.write(l.getBody());
				writer.newLine();
				writer.flush();
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, e.toString(), e);
			}
		});
	}

	private void startWriter(final InputStream in) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		new Thread() {
			@Override
			public void run() {
				try {
					while (true) {
						send(reader.readLine());
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, e.toString(), e);
				}
			}
		}.start();
	}

	public Messenger waitBrowser() throws InterruptedException {
		synchronized (browser) {
			browser.wait();
		}
		return this;
	}

	public Messenger closeBrowser() {
		browser.close();
		return this;
	}

	private void startListener() {
		new Thread() {
			@Override
			public void run() {
				while (true) {
					int messagesSize = getNewMessages(browser).size();
					new FluentWait<>(browser)
							.withTimeout(Duration.ofDays(1))
							.pollingEvery(Duration.ofSeconds(1))
							.until(new ExpectedCondition<Boolean>() {
								@Override
								public Boolean apply(final WebDriver driverObject) {
									return messagesSize < getNewMessages(browser).size();
								}
							});
					List<WebElement> messages = getNewMessages(browser);
					for (int i = messagesSize; i < messages.size(); i++) {
						WebElement webElement = messages.get(i);
						Message m = createMessage(webElement);
						messageListeners.forEach(l -> l.onReceive(m));
					}
				}

			}
		}.start();
	}

	private String getFriendName(final WebElement r) {
		return r.findElement(By.tagName("h3")).getText();
	}

	private List<WebElement> getFriendsRows() {
		List<WebElement> l = browser.findElements(By.xpath("//div[@id='threadlist_rows']/div/div"));
		return l;
	}

	private WebElement getFriendRow(final String friendName) {
		return getFriendsRows().stream().filter(r -> getFriendName(r).startsWith(friendName)).findFirst().get();
	}

	private WebElement waitForThreadListRows() {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(threadListRows));
	}

	private Message createMessage(final WebElement webElement) {
		Message m = new Message();
		m.setBody(webElement.getText());
		m.setDateString("");
		m.setType(webElement.getAttribute("class").contains("_34em") ? Type.OUT : Type.IN);
		return m;
	}

	private List<WebElement> getMessages(final WebDriver browser) {
		List<WebElement> res = browser.findElements(By.xpath("//div[@data-sigil='message-text']/span/div"));
		return res;
	}

	private List<WebElement> getNewMessages(final WebDriver browser) {
		List<WebElement> res = browser.findElements(By.xpath("//div[@dir='auto']/.."));
		return res;
	}
}
