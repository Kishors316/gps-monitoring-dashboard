package com.gpstracker.report.model;

/** One row of device_details. */
public class Device {
    public String deviceId;   // device_details.device_id  == locations.device_id
    public String refName;    // device_details.ref_name
    public String lastUpdate; // device_details.last_update (formatted, may be null)

    public Device(String deviceId, String refName, String lastUpdate) {
        this.deviceId = deviceId;
        this.refName = refName;
        this.lastUpdate = lastUpdate;
    }
}
