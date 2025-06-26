package Pages;

import BaseClass.BaseTest;
import com.google.common.collect.ImmutableMap;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WrapPage extends BaseTest {

    public WrapPage()
    {
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    public void GetText()
    {
        System.out.println("in func");
    }



}
