package io.fsight.pvic.service;

import io.fsight.pvic.model.Device;
import io.fsight.pvic.util.FileUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InformationCollectorService {
    private static final String SUNNYPORTAL = "https://www.sunnyportal.com";
    private static final String SUNNY_LOGIN = "nir.nitzani@fsight.io";
    private static final String SUNNY_PASS = "dopacafu";

    private static final String SUNNY_ANALYSIS_PAGE = "https://www.sunnyportal.com/FixedPages/AnalysisTool.aspx";

    private static final List<String> DEVICES = Arrays.asList("SMC 10000TL 019", "SMC 10000TL 042", "SMC 6000TL 031",
            "SMC 6000TL 795", "SMC 9000TL 105", "SMC 9000TL 503",
            "STP 17000TL-10 431", "STP 17000TL-10 435", "STP 17000TL-10 561");

    private static LocalDate date = LocalDate.now();

    public void runScraping() throws InterruptedException, IOException {
        FileUtil.createCSV(collectInformationFromSunnyPortal());
    }

    private List<Device> collectInformationFromSunnyPortal() throws InterruptedException {
        System.out.println("get resource");
        DesiredCapabilities caps = new DesiredCapabilities();
        List<Device> devices = new ArrayList<>();
        caps.setJavascriptEnabled(true);
        caps.setCapability("takesScreenshot", true);
        caps.setCapability("phantomjs.binary.path", System.getProperty("user.dir") + "/phantomjs/bin/phantomjs");
        WebDriver driver = new PhantomJSDriver(caps);

        driver.get(SUNNYPORTAL);
        //login
        System.out.println("logging");
        driver.findElement(By.id("txtUserName")).sendKeys(SUNNY_LOGIN);
        driver.findElement(By.id("txtPassword")).sendKeys(SUNNY_PASS);
//        driver.findElement(By.id("ctl00_ContentPlaceHolder1_Logincontrol1_MemorizePassword")).click();

        driver.findElement(By.name("ctl00$ContentPlaceHolder1$Logincontrol1$LoginBtn")).click();

        driver.navigate().to(SUNNY_ANALYSIS_PAGE);
        //select all devices
        driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_DeviceSelection_SelectAllCheckBox")).click();
        Thread.sleep(7000); //waiting for loading
        String page = driver.getPageSource();
        while(date.isAfter(LocalDate.now().minus(2, ChronoUnit.YEARS))) {
            WebElement oneDayBack = driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_ChartLeftButton_ImageButton"));
            oneDayBack.click();
            devices.addAll(parseData(driver));
        }
        driver.close();
        return devices;
    }

    private List<Device> parseData(WebDriver driver) throws InterruptedException {
        List<Device> devices = new ArrayList<>();
        if(driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_ChartLeftButton_ImageButton"))
                .getAttribute("disabled") != null) {
            Thread.sleep(7000);
            date = date.minus(1, ChronoUnit.DAYS);
            System.out.println(date);
            List<WebElement> trs = getAndCheckTrs(driver);
            List<Device> dallyReport = parseDevices(trs, date);
            devices.addAll(dallyReport);
        } else {
            updateData(driver);
            parseData(driver);
        }
        return devices;
    }

    private List<Device> parseDevices(List<WebElement> trs, LocalDate date) {
        List<Device> devices = new ArrayList<>();
        Map<String, List<WebElement>> tdsByTime = new HashMap<>();
        try {
            tdsByTime = trs
                    .stream()
                    .skip(1)
                    .map(tr -> tr.findElements(By.tagName("td")))
                    .collect(Collectors.toMap(tds -> tds.get(0).getAttribute("innerHTML"), tds -> tds, (a, b) -> b));
        } catch (StaleElementReferenceException ex) {
            return devices;
        }

        for(String time : tdsByTime.keySet()) {
            List<WebElement> tds = tdsByTime.get(time).subList(2, 11);
            tds.forEach(td -> {
                String value = td.getAttribute("innerHTML");
                if(!value.equals("") && !value.contains(":")) {
                    devices.add(new Device(DEVICES.get(tds.indexOf(td)), LocalDateTime.of(date, LocalTime.parse(time)), value));
                }
            });
        }

        return devices;
    }

    private List<WebElement> getAndCheckTrs(WebDriver driver) throws InterruptedException {
        List<WebElement> trs = getTrs(driver);
        List<WebElement> firstTds = trs.stream().map(tr -> tr.findElements(By.tagName("td"))).findFirst().get();
        if(firstTds.size() != 11) {
            getAndCheckTrs(updateData(driver));
        }
        return trs;
    }

    private WebDriver updateData(WebDriver driver) throws InterruptedException {
        WebElement checkBox = driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_DeviceSelection_SelectAllCheckBox"));
        if(checkBox.getAttribute("checked") != null) {
            checkBox.click();
        }
        Thread.sleep(7000);
        driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_ChartLeftButton_ImageButton")).click();
        Thread.sleep(7000);
        driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_ChartRightButton_ImageButton")).click();
        Thread.sleep(7000);
        return driver;
    }

    private List<WebElement> getTrs(WebDriver driver) {
        return driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_ChartDetailSliderTab_ChartDetails_ChartDetailTable")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));

    }
}
