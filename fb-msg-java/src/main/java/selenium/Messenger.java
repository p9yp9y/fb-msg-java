package selenium;

import static java.util.stream.Collectors.toList;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Messenger {

	private ChromeDriver browser;
	private WebDriverWait wait;

	public Messenger() {
		init();
	}

	private void init() {
		System.setProperty("webdriver.chrome.driver", "/usr/lib/chromium-browser/chromedriver");

		ChromeOptions opt = new ChromeOptions();

		//opt.addArguments("user-data-dir=/home/andris/Asztal/testprof");
		
		opt.setBinary("/usr/bin/chromium-browser");

		browser = new ChromeDriver(opt);

		wait = new WebDriverWait(browser, 10);
	}

	public void connect(String email, String password) {
		browser.get("https://m.facebook.com/messages/");
		WebElement e = browser.findElement(By.id("m_login_email"));
		e.sendKeys(email);
		e = browser.findElement(By.id("m_login_password"));
		e.sendKeys(password);

		e = browser.findElement(By.xpath("//div[@data-sigil='login_password_step_element']/button"));
		e.click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("threadlist_rows")));

	}

	public List<String> listFriends() {
		return getFriendsRows().stream().map(r -> getFriendName(r)).collect(toList());
	}

	public void selectFriend(String friendName) {
		getFriendRow(friendName).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("_z3m")));

		List<WebElement> l = browser.findElements(By.className("_z3m"));

		l.forEach(r -> {
			System.out.println("text: " + r.getText());
		});
	}

	public void send(String message) {
		WebElement e = browser.findElement(By.id("composerInput"));
		e.sendKeys(message);

		e = browser.findElement(By.xpath("//button[@name='send']"));
		e.click();
	}

	public void waitBrowser() throws InterruptedException {
		browser.wait();
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
}
