package Tests;

import BaseClass.BaseTest;
import Pages.HomePage;
import Pages.WrapPage;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.testng.annotations.*;

import java.io.*;
import java.lang.reflect.Method;

public class HomeTests extends BaseTest {

    HomePage homePage;
    WrapPage wrapPage;
    JSONObject logindata;

    @BeforeMethod
    public void beforemethod(Method m)
    {
        //bar bar har test ke liye initialize na krna pare isiliye hook ma initialize krdia
        homePage = new HomePage();
        System.out.println(m.getName());
    }

    @Test(priority = 1)
    public void Scenario1() throws InterruptedException {
        wrapPage = homePage.Click_Wrap();
    }


}
