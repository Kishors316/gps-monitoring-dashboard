package com.gpstracker.report.web;

import com.gpstracker.report.db.Db;
import com.gpstracker.report.db.ReportDao;
import com.gpstracker.report.model.Device;
import com.gpstracker.report.model.LocationPoint;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * JSON API:
 *   GET /api/devices
 *   GET /api/locations?deviceId=..&from=<datetime-local>&to=<datetime-local>
 *
 * 'from'/'to' arrive as HTML datetime-local strings (yyyy-MM-dd'T'HH:mm),
 * interpreted in the SERVER's local time zone and converted to epoch millis
 * to match locations.fix_time.
 */
public class ApiServlet extends HttpServlet {

    private final ReportDao dao = new ReportDao();
    private static final DateTimeFormatter LOCAL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    //droneaeromatix.com
    //localhost
    
    /** Read DB settings from web.xml context-params and configure the driver. */
    @Override
    public void init() {
        ServletContext c = getServletContext();
        Db.configure(
            p(c, "dbHost", "localhost"),
            p(c, "dbPort", "3306"),
            p(c, "dbName", "database_name"),
            p(c, "dbUser", "User"),
            p(c, "dbPass", "Password"));
    }

    private static String p(ServletContext c, String key, String def) {
        String v = c.getInitParameter(key);
        return (v == null || v.isEmpty()) ? def : v;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        try {
            if ("/devices".equals(path)) {
                writeDevices(resp);
            } else if ("/locations".equals(path)) {
                writeLocations(req, resp);
            } else {
                error(resp, 404, "Unknown endpoint");
            }
        } catch (IllegalArgumentException ex) {
            error(resp, 400, ex.getMessage());
        } catch (Exception ex) {
            error(resp, 500, "Server error: " + ex.getMessage());
        }
    }

    private void writeDevices(HttpServletResponse resp) throws Exception {
        List<Device> list = dao.devices();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            Device d = list.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"deviceId\":").append(Json.s(d.deviceId))
              .append(",\"refName\":").append(Json.s(d.refName))
              .append(",\"lastUpdate\":").append(Json.s(d.lastUpdate))
              .append('}');
        }
        sb.append(']');
        resp.getWriter().write(sb.toString());
    }

    private void writeLocations(HttpServletRequest req, HttpServletResponse resp)
            throws Exception {
        String deviceId = req.getParameter("deviceId");
        String from = req.getParameter("from");
        String to = req.getParameter("to");
        if (isBlank(deviceId) || isBlank(from) || isBlank(to)) {
            throw new IllegalArgumentException("deviceId, from and to are required");
        }
        long fromMs = toMillis(from);
        long toMs = toMillis(to);
        if (toMs < fromMs) throw new IllegalArgumentException("'to' is before 'from'");

        List<LocationPoint> pts = dao.locations(deviceId, fromMs, toMs);

        double distKm = totalDistanceKm(pts);
        double maxKmh = 0;
        for (LocationPoint p : pts) maxKmh = Math.max(maxKmh, p.speed);   // speed already km/h

        // Expected fixes = span between the first and last actual record, in whole
        // minutes (device reports once per minute). Uses min/max so it is correct
        // regardless of the row sort order.
        long expectedCount = 0;
        if (pts.size() >= 2) {
            long minT = Long.MAX_VALUE, maxT = Long.MIN_VALUE;
            for (LocationPoint p : pts) {
                if (p.fixTime < minT) minT = p.fixTime;
                if (p.fixTime > maxT) maxT = p.fixTime;
            }
            expectedCount = Math.round((maxT - minT) / 60000.0);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"count\":").append(pts.size())
          .append(",\"expectedCount\":").append(expectedCount)
          .append(",\"distanceKm\":").append(round(distKm, 3))
          .append(",\"maxSpeedKmh\":").append(round(maxKmh, 1))
          .append(",\"points\":[");
        for (int i = 0; i < pts.size(); i++) {
            LocationPoint p = pts.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"fixTime\":").append(p.fixTime)
              .append(",\"fixTimeIso\":").append(Json.s(p.fixTimeIso))
              .append(",\"latitude\":").append(p.latitude)
              .append(",\"longitude\":").append(p.longitude)
              .append(",\"speed\":").append(p.speed)
              .append(",\"accuracy\":").append(p.accuracy == null ? "null" : p.accuracy.toString())
              .append(",\"battery\":").append(p.battery == null ? "null" : p.battery.toString())
              .append(",\"gpsState\":").append(p.gpsState == null ? "null" : p.gpsState.toString())
              .append(",\"internetState\":").append(p.internetState == null ? "null" : p.internetState.toString())
              .append(",\"flightState\":").append(p.flightState == null ? "null" : p.flightState.toString())
              .append(",\"roamingState\":").append(p.roamingState == null ? "null" : p.roamingState.toString())
              .append(",\"isNetThere\":").append(p.isNetThere == null ? "null" : p.isNetThere.toString())
              .append(",\"isNwThere\":").append(p.isNwThere == null ? "null" : p.isNwThere.toString())
              .append(",\"datatype\":").append(p.datatype == null ? "null" : p.datatype.toString())
              .append(",\"observations\":").append(Json.s(p.observations))
              .append('}');
        }
        sb.append("]}");
        resp.getWriter().write(sb.toString());
    }

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private static long toMillis(String datetimeLocal) {
        try {
            LocalDateTime ldt = LocalDateTime.parse(datetimeLocal, LOCAL);
            return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            throw new IllegalArgumentException("Bad datetime: " + datetimeLocal);
        }
    }

    private static double totalDistanceKm(List<LocationPoint> pts) {
        double sum = 0;
        for (int i = 1; i < pts.size(); i++) {
            sum += haversineKm(pts.get(i - 1).latitude, pts.get(i - 1).longitude,
                               pts.get(i).latitude, pts.get(i).longitude);
        }
        return sum;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static double round(double v, int dp) {
        double f = Math.pow(10, dp);
        return Math.round(v * f) / f;
    }

    private void error(HttpServletResponse resp, int code, String msg) throws IOException {
        resp.setStatus(code);
        PrintWriter w = resp.getWriter();
        w.write("{\"error\":" + Json.s(msg) + "}");
    }
}