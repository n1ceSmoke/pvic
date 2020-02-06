package io.fsight.pvic.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Device implements Comparable<Device>{
    private String id;
    private LocalDateTime time;
    private String power;

    public boolean isEmpty() {
        return id == null && time == null && power == null;
    }

    @Override
    public int compareTo(Device o) {
        return this.time.compareTo(o.time);
    }
}
