package BaseClass;

import io.appium.java_client.*;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.*;

import java.io.*;
import java.net.URL;
import java.util.*;

//use this listener along with with html-config.xml and extent properties to generate default reports
//@Listeners(ExtentITestListenerClassAdapter.class)
public class BaseTest {

    public static String devicename;
    protected static AppiumDriver driver;
    protected static String dateTime;
    protected Properties properties;
    protected static HashMap<String, String> strings = new HashMap<String, String>();
    InputStream inputStream;
    InputStream stringxml;
    //start appium server programmatically instead of starting it manually
    private static AppiumDriverLocalService server;

    @BeforeSuite
    public void beforeSuite()
    {
        server = getAppiumServerDefault();
        server.start();
    }

    @AfterSuite
    public void afterSuite() throws InterruptedException {
        Thread.sleep(3000);
        server.stop();
    }

    public AppiumDriverLocalService getAppiumServerDefault()
    {
        return AppiumDriverLocalService.buildDefaultService();
    }

    //if the above method does not work

    public String getDeviceName()
    {
        return devicename;
    }


    @Parameters({"platformName", "deviceName", "udid"})
    @BeforeTest
    public void BeforeTestHook(String platformName, String deviceName, String udid) throws Exception {


        try{
            devicename = deviceName;
            properties = new Properties();
            String propertiesFilename = "config.properties";
            String xmlFilename = "strings.xml";
            inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFilename);
            stringxml = getClass().getClassLoader().getResourceAsStream(xmlFilename);

            properties.load(inputStream);

            DesiredCapabilities capabilities = new DesiredCapabilities();
            capabilities.setCapability("platformName", platformName);
            capabilities.setCapability("appium:automationName", properties.getProperty("androidAutomationName"));
            capabilities.setCapability("appium:deviceName", deviceName);
            capabilities.setCapability("appium:udid", udid);
            capabilities.setCapability("appium:app", System.getProperty("user.dir")+ File.separator+"src"+File.separator+"main"+File.separator+"resources"+File.separator+"App"+File.separator+"draganddrop.apk");

            URL url = new URL(properties.getProperty("AppiumURL"));

            driver = new AndroidDriver(url, capabilities);
            System.out.println(driver.getSessionId());

        } catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }finally {
            if(inputStream!=null)
            {
                inputStream.close();
            }
            if(stringxml!=null)
            {
                stringxml.close();
            }
        }
    }

    public void Click(WebElement element, String msg)
    {
        element.click();
    }


}

