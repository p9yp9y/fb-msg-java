package selenium;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class Messenger {
	public void connect(String email, String password) {
		System.setProperty("webdriver.chrome.driver", "/usr/lib/chromium-browser/chromedriver");

		ChromeOptions opt = new ChromeOptions();
		opt.setBinary("/usr/bin/chromium-browser");

		WebDriver browser = new ChromeDriver(opt);
		browser.get("https://m.facebook.com/messages/");
		WebElement e = browser.findElement(By.id("m_login_email"));
		e.sendKeys(email);
		e = browser.findElement(By.id("m_login_password"));
		e.sendKeys(password);

		e = browser.findElement(By.xpath("//div[@data-sigil='login_password_step_element']/button"));
		e.click();

		WebDriverWait wait = new WebDriverWait(browser, 10);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("threadlist_rows")));

		List<WebElement> l = browser.findElements(By.xpath("//div[@id='threadlist_rows']/div/div"));

		l.forEach(r -> {
			r = r.findElement(By.xpath("//header/h3"));
			System.out.println("text: " + r.getText());
		});

		l.get(0).click();

		wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("_z3m")));

		l = browser.findElements(By.className("_z3m"));

		l.forEach(r -> {
			System.out.println("text: " + r.getText());
		});

		browser.close();
		
//		try {
//			browser.wait();
//		} catch (InterruptedException e1) {
//			e1.printStackTrace();
//		}
	}
}
