package io.fsight.pvic.service;

import io.fsight.pvic.model.Device;
import io.fsight.pvic.util.FileUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScheduledService {
    private static final String SUNNYPORTAL = "https://www.sunnyportal.com";
    private static final String SUNNY_LOGIN = "nir.nitzani@fsight.io";
    private static final String SUNNY_PASS = "dopacafu";

    private static final String SUNNY_ANALYSIS_PAGE = "https://www.sunnyportal.com/FixedPages/AnalysisTool.aspx";

    private static final List<String> DEVICES = Arrays.asList("SMC 10000TL 019", "SMC 10000TL 042", "SMC 6000TL 031",
            "SMC 6000TL 795", "SMC 9000TL 105", "SMC 9000TL 503",
            "STP 17000TL-10 431", "STP 17000TL-10 435", "STP 17000TL-10 561");

    @Scheduled(cron = "0 30 23 * * *")
    public void getActualData() throws InterruptedException, IOException {
        System.out.println("Scheduled start");
        List<Device> devices = new ArrayList<>();
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setJavascriptEnabled(true);
        caps.setCapability("takesScreenshot", true);
        caps.setCapability("phantomjs.binary.path", System.getProperty("user.dir") + "/phantomjs/bin/phantomjs");
        WebDriver driver = new PhantomJSDriver(caps);

        driver.get(SUNNYPORTAL);
        //login

        driver.findElement(By.id("txtUserName")).sendKeys(SUNNY_LOGIN);
        driver.findElement(By.id("txtPassword")).sendKeys(SUNNY_PASS);

        driver.findElement(By.name("ctl00$ContentPlaceHolder1$Logincontrol1$LoginBtn")).click();
        navigateToAndSelectAllDevices(driver);

        String page = driver.getPageSource();
        List<WebElement> trs = getAndCheckTrs(driver);
        Thread.sleep(7000); //waiting for loading

        devices.addAll(parseDevices(trs));
        driver.close();

        FileUtil.createCSV(devices);
        System.out.println("Scheduled successfully ended");
    }

    private List<Device> parseDevices(List<WebElement> trs) {
        List<Device> devices = new ArrayList<>();
        LocalDate date = LocalDate.now();
        Map<String, List<WebElement>> tdsByTime = trs
                .stream()
                .skip(1)
                .map(tr -> tr.findElements(By.tagName("td")))
                .collect(Collectors.toMap(tds -> tds.get(0).getAttribute("innerHTML"), tds -> tds, (a, b) -> b));

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
            navigateToAndSelectAllDevices(driver);
            getAndCheckTrs(driver);
        }
        return trs;
    }

    private List<WebElement> getTrs(WebDriver driver) {
        return driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_ChartDetailSliderTab_ChartDetails_ChartDetailTable")).findElement(By.tagName("tbody")).findElements(By.tagName("tr"));

    }

    private void navigateToAndSelectAllDevices(WebDriver driver) throws InterruptedException {
        driver.navigate().to(SUNNY_ANALYSIS_PAGE);

        driver.findElement(By.id("ctl00_ContentPlaceHolder1_UserControlShowAnalysisTool1_DeviceSelection_SelectAllCheckBox")).click();
        Thread.sleep(7000); //waiting for loading
    }
}
