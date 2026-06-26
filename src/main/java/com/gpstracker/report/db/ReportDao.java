package com.gpstracker.report.db;

import com.gpstracker.report.model.Device;
import com.gpstracker.report.model.LocationPoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Devices come from `device_details` (friendly ref_name); location history from
 * `data_yday_avt`. Link: device_details.device_id == data_yday_avt.imei. Both
 * tables live in the configured database.
 *
 * data_yday_avt mapping:
 *   lat / lng                  -> latitude / longitude
 *   gpsdatetime                -> fix time (DATETIME, wall-clock)
 *   speed                      -> speed (m/s as stored; UI multiplies by 3.6)
 *   intbat                     -> battery %
 *   extbat                     -> accuracy (m)   (receiver stores GPS accuracy here)
 *   GpsState / InternetState /
 *   FlightState / RoamingState /
 *   IsNetThere / IsNwThere     -> device-state flags (0/1) shown as Yes/No
 */
public class ReportDao {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** All devices from device_details, alphabetical by reference name. */
    public List<Device> devices() throws SQLException {
        String sql = "SELECT device_id, ref_name, last_update "
                   + "FROM device_details ORDER BY ref_name";
        List<Device> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Timestamp lu = rs.getTimestamp("last_update");
                out.add(new Device(
                        rs.getString("device_id"),     // == data_yday_avt.imei
                        rs.getString("ref_name"),
                        lu == null ? null : DT.format(lu.toLocalDateTime())));
            }
        }
        return out;
    }

    /** History for one imei within [fromMillis, toMillis], newest first. */
    public List<LocationPoint> locations(String deviceId, long fromMillis, long toMillis)
            throws SQLException {
        String from = DT.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(fromMillis), ZONE));
        String to   = DT.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(toMillis), ZONE));

        String sql = "SELECT lat, lng, gpsdatetime, speed, intbat, extbat, "
                   + "GpsState, InternetState, FlightState, RoamingState, IsNetThere, IsNwThere, datatype, observations "
                   + "FROM data_yday_avt "
                   + "WHERE imei = ? AND gpsdatetime BETWEEN ? AND ? "
                   + "ORDER BY gpsdatetime DESC";
        List<LocationPoint> out = new ArrayList<>();
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, deviceId);                  // device_details.device_id == imei
            ps.setString(2, from);
            ps.setString(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocationPoint p = new LocationPoint();
                    LocalDateTime ldt = parse(rs.getString("gpsdatetime"));
                    if (ldt != null) {
                        p.fixTime = ldt.atZone(ZONE).toInstant().toEpochMilli();
                        p.fixTimeIso = DT.format(ldt);
                    } else {
                        p.fixTime = 0L;
                        p.fixTimeIso = "";
                    }
                    p.latitude = rs.getDouble("lat");
                    p.longitude = rs.getDouble("lng");
                    p.speed = rs.getDouble("speed");      // m/s
                    p.battery       = nint(rs, "intbat");
                    p.accuracy      = ndbl(rs, "extbat");   // accuracy now lives in extbat
                    p.gpsState      = nint(rs, "GpsState");
                    p.internetState = nint(rs, "InternetState");
                    p.flightState   = nint(rs, "FlightState");
                    p.roamingState  = nint(rs, "RoamingState");
                    p.isNetThere    = nint(rs, "IsNetThere");
                    p.isNwThere     = nint(rs, "IsNwThere");
                    p.datatype      = nint(rs, "datatype");
                    p.observations  = rs.getString("observations");
                    out.add(p);
                }
            }
        }
        return out;
    }

    /** Read an int column as a nullable Integer. */
    private static Integer nint(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    /** Read a double column as a nullable Double. */
    private static Double ndbl(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    private static LocalDateTime parse(String sqlDateTime) {
        if (sqlDateTime == null) return null;
        String s = sqlDateTime.trim();
        if (s.length() > 19) s = s.substring(0, 19);   // drop any fractional seconds
        try {
            return LocalDateTime.parse(s, DT);
        } catch (Exception e) {
            return null;
        }
    }
}