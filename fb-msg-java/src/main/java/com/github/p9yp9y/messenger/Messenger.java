package com.github.p9yp9y.messenger;

import static java.util.stream.Collectors.toList;

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
import org.openqa.selenium.support.ui.WebDriverWait;

import com.beust.jcommander.Parameter;

import pgy.master.util.JCommanderUtil;

public class Messenger {
	@Parameter(names = { "-method" }, required = true)
	private String method;

	@Parameter(names = { "-email" })
	private String email;

	@Parameter(names = { "-password" })
	private String password;

	@Parameter(names = { "-message" })
	private String message;

	@Parameter(names = { "-friendName" })
	private String friendName;

	private static final String MESSAGES_URL = "https://m.facebook.com/messages/";
	private By threadListRows = By.id("threadlist_rows");
	private final static Logger LOGGER = Logger.getLogger(Messenger.class.getName());
	private ChromeDriver browser;
	private WebDriverWait wait;

	public Messenger() {
		this(new String[] {});
	}

	public Messenger(String[] args) {
		JCommanderUtil.parseArgs(this, args);
		init();
		exec();
	}

	public static void main(String[] args) {
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
		} else {
			throw new RuntimeException("This method not found: " + method);
		}
	}

	private void init() {
		System.setProperty("webdriver.chrome.driver", "/usr/lib/chromium-browser/chromedriver");

		ChromeOptions opt = new ChromeOptions();

		opt.addArguments("user-data-dir=/home/andris/Asztal/testprof");

		opt.setBinary("/usr/bin/chromium-browser");

		browser = new ChromeDriver(opt);

		wait = new WebDriverWait(browser, 10);
	}

	public void login(String email, String password) {
		WebElement e;
		browser.get(MESSAGES_URL);
		try {
			e = browser.findElement(By.id("m_login_email"));
			e.sendKeys(email);
		} catch (NoSuchElementException exc) {
			try {
				e = browser.findElement(By.tagName("form"));
			} catch (NoSuchElementException exc2) {
				return;
			}
			if (e.getAttribute("action").contains("/login/device-based/validate-pin/")) {
				e.submit();
			} else {
				return;
			}
		}

		e = browser.findElement(By.xpath("//input[@type='password']"));
		e.sendKeys(password);

		e = browser.findElement(By.xpath("//button"));
		e.click();

		waitForThreadListRows();
	}

	public void logout() {
		browser.manage().deleteAllCookies();
	}

	public List<String> listFriends() {
		return getFriendsRows().stream().map(r -> getFriendName(r)).collect(toList());
	}

	public void startConversation(String friendName) {
		try {
			browser.findElement(threadListRows);
		} catch (NoSuchElementException exc) {
			browser.get(MESSAGES_URL);
			waitForThreadListRows();
		}

		getFriendRow(friendName).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("_z3m")));

		List<WebElement> l = browser.findElements(By.xpath("//div[@class='msg']/div/span/div/div"));

		LOGGER.log(Level.INFO, "Messages: " + l.size());

		l.forEach(r -> {
			LOGGER.log(Level.INFO, "text: " + r.getText());
		});

		startListener();
	}

	private void startListener() {
		new Thread() {
			public void run() {
				while (true) {
					int messagesSize = getMessages(browser).size();
					new WebDriverWait(browser, 1000000, 500).until(new ExpectedCondition<Boolean>() {
						@Override
						public Boolean apply(WebDriver driverObject) {

							return messagesSize < getMessages(driverObject).size();
						}
					});
					LOGGER.log(Level.WARNING, "!!!!!!!!!!!!!!!!!!!!!!");
				}

			}
		}.start();
	}

	private List<WebElement> getMessages(WebDriver browser) {
		return browser.findElements(By.xpath("//div[@class='msg']/div/span/div/div"));
	}

	public void send(String message) {
		WebElement e = browser.findElement(By.id("composerInput"));
		e.sendKeys(message);

		e = browser.findElement(By.xpath("//button[@name='send']"));
		e.click();
	}

	public void waitBrowser() throws InterruptedException {
		synchronized (browser) {
			browser.wait();
		}
	}

	public void closeBrowser() {
		browser.close();
	}

	private String getFriendName(WebElement r) {
		return r.findElement(By.tagName("h3")).getText();
	}

	private List<WebElement> getFriendsRows() {
		List<WebElement> l = browser.findElements(By.xpath("//div[@id='threadlist_rows']/div/div"));
		return l;
	}

	private WebElement getFriendRow(String friendName) {
		return getFriendsRows().stream().filter(r -> getFriendName(r).equals(friendName)).findFirst().get();
	}

	private WebElement waitForThreadListRows() {
		return wait.until(ExpectedConditions.visibilityOfElementLocated(threadListRows));
	}
}
