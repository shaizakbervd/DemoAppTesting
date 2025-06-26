package Pages;

import BaseClass.BaseTest;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.appium.java_client.pagefactory.AppiumFieldDecorator;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

public class HomePage extends BaseTest
{

    public HomePage()
    {
        PageFactory.initElements(new AppiumFieldDecorator(driver), this);
    }

    @AndroidFindBy (xpath = "//android.widget.TextView[@resource-id=\"com.mobeta.android.demodslv:id/activity_title\" and @text=\"Warp\"]") private WebElement Wrap;


    public WrapPage Click_Wrap()
    {
        Click(Wrap, "Tapping on wrap button");
        return new WrapPage();
    }

}

