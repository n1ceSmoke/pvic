package io.fsight.pvic.util;

import io.fsight.pvic.model.Device;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class FileUtil {

    private static final String[] HEADERS = {"DeviceID", "Time", "Power(kW)"};

    public static void createCSV(List<Device> devices) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        File file = new File(System.getProperty("user.dir") + "/PV generation " + LocalDate.now() + ".csv");
        if(!file.exists()) {
            file.createNewFile();
        }
        FileOutputStream fos = new FileOutputStream(file);
        try(BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
            CSVPrinter csvPrinter = new CSVPrinter(bf, CSVFormat.DEFAULT.withHeader(HEADERS))) {
            Collections.sort(devices);
            for(Device device : devices) {
                csvPrinter.printRecord(getValues(device));
            }

            csvPrinter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        baos.writeTo(fos);
    }

    private static String[] getValues(Device device) {
        if(device != null && !device.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            return new String[] {device.getId(), formatter.format(device.getTime()), device.getPower()};
        }
        return new String[] {};
    }

}
