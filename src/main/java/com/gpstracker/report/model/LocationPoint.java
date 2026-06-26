package com.gpstracker.report.model;

/** One row of data_yday_avt, shaped for the report/map. */
public class LocationPoint {
    public long fixTime;       // epoch millis
    public String fixTimeIso;  // human-readable (server local zone)
    public double latitude;
    public double longitude;
    public double speed;       // m/s as stored
    public Integer battery;    // may be null
    public Double  accuracy;   // metres, from data_yday_avt.extbat; may be null

    // device-state flags from data_yday_avt (0/1, may be null) -> shown as Yes/No
    public Integer gpsState;
    public Integer internetState;
    public Integer flightState;
    public Integer roamingState;
    public Integer isNetThere;
    public Integer isNwThere;
    public Integer datatype;   // 1 = first record after login, 2 = normal
    public String  observations; // device-health summary ("All Ok" or issues)
}