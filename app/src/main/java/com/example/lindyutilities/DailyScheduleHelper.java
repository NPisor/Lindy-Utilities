package com.example.lindyutilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DailyScheduleHelper {

    private static final String CHANNEL_ID = "schedule_updates";
    private static final int NOTIFICATION_ID = 1;

    private final String url = "https://scheduling.lindypaving.com/tpjwebsite.nsf/xhtmlDailySchedule?openForm";
    private final String employeeId;
    private final OkHttpClient client;
    private String lastHash = null; // To store the last hash of the schedule
    private ScheduledExecutorService scheduler;
    private final Context context;

    public DailyScheduleHelper(String employeeId, Context context) {
        this.employeeId = employeeId;
        this.client = new OkHttpClient();
        this.context = context;
        createNotificationChannel();
    }

    private String extractScheduleDate(String html) {
        Document doc = Jsoup.parse(html);
        Element header = doc.selectFirst("h3 span.dailySchedule"); // Select the `<span>` in the `<h3>`
        if (header != null) {
            String fullText = header.parent().text(); // Get text from the `<h3>` parent
            return fullText.replace("Daily Schedule for ", "").trim(); // Extract and clean up the date
        }
        return "Not Found";
    }

    /**
     * Fetches the schedule and provides the result through the callback.
     *
     * @param callback Callback to handle the result.
     */
    public void fetchSchedule(ScheduleCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", "schedulingEmpID=" + employeeId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String html = response.body().string();
                    String scheduleDate = extractScheduleDate(html);
                    Document doc = Jsoup.parse(html);

                    // Initialize "mySchedule" with a default
                    Employee[] mySchedule = new Employee[1];

                    // Parse all employees, including "You" (from `current` row)
                    Element employeesTable = doc.select("table.dailySchedule").get(1);
                    List<Employee> employees = parseEmployees(employeesTable, mySchedule);

                    Log.d("MySchedule", mySchedule[0].toString());

                    callback.onSuccess(scheduleDate, mySchedule[0], employees); // Pass both results
                } else {
                    callback.onError("Error: HTTP " + response.code());
                }
            }
        });
    }


    /**
     * Starts periodic checking for updates.
     * @param interval Interval in seconds between checks.
     */
    public void startCheckingForUpdates(long interval) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkForUpdates, 0, interval, TimeUnit.SECONDS);
    }

    /**
     * Stops periodic checking for updates.
     */
    public void stopCheckingForUpdates() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    /**
     * Checks for schedule updates and sends a notification if changes are detected.
     */
    private void checkForUpdates() {
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Cookie", "schedulingEmpID=" + employeeId)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Log or handle errors if necessary
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String html = response.body().string();
                    String currentHash = hashContent(html);

                    if (lastHash == null || !currentHash.equals(lastHash)) {
                        lastHash = currentHash; // Update the stored hash
                        sendNotification();
                    }
                }
            }
        });
    }

    /**
     * Computes a hash of the content to detect changes.
     */
    private String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashBytes = digest.digest(content.getBytes());
            StringBuilder hashBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                hashBuilder.append(String.format("%02x", b));
            }
            return hashBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * Sends a notification to notify the user about schedule updates.
     */
    private void sendNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setContentTitle("Schedule Updated")
                .setContentText("Your schedule has been updated. Tap to view.")
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /**
     * Creates a notification channel for schedule updates (required for Android 8.0+).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Schedule Updates";
            String description = "Notifications for schedule updates.";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Extracts employee data from the given table element.
     *
     * @param table The HTML table element containing employee rows.
     * @return A list of Employee objects parsed from the table.
     */
    private List<Employee> parseEmployees(Element table, Employee[] mySchedule) {
        List<Employee> employees = new ArrayList<>();
        Employee currentEmployee = null; // To associate address with the last employee

        // Extract all rows from the table
        Elements rows = table.select("tr");

        for (int i = 0; i < rows.size(); i++) {
            Element row = rows.get(i);

            // Handle your personal entry (row with "current" class)
            if (row.hasClass("current")) {
                currentEmployee = parseEmployeeDetails(row);

                // Look ahead for the job address
                String jobAddress = "N/A";
                if (i + 1 < rows.size() && rows.get(i + 1).select(".dailySchedule.pnm-comments").first() != null) {
                    jobAddress = rows.get(i + 1).select(".dailySchedule.pnm-comments").text();
                    jobAddress = jobAddress.replace("Job Address: ", "").trim(); // Clean up address
                    i++; // Skip the address row
                }
                currentEmployee.setJobAddress(jobAddress);

                Log.d("EmployeeDetails", currentEmployee.toString());

                // Assign this employee as "You"
                mySchedule[0] = currentEmployee;
            }
            // Handle other employees (rows with "empRow" class)
            else if (row.hasClass("empRow")) {
                currentEmployee = parseEmployeeDetails(row);

                // Look ahead for the job address
                String jobAddress = "N/A";
                if (i + 1 < rows.size() && rows.get(i + 1).select(".dailySchedule.pnm-comments").first() != null) {
                    jobAddress = rows.get(i + 1).select(".dailySchedule.pnm-comments").text();
                    i++; // Skip the address row
                }
                currentEmployee.setJobAddress(jobAddress);

                // Add to employee list
                employees.add(currentEmployee);
            }
        }

        // If no "current" row was found, ensure mySchedule has a default value
        if (mySchedule[0] == null) {
            mySchedule[0] = new Employee("You", "N/A", "Not scheduled today", "N/A", "N/A", "N/A", "N/A", "N/A");
        }

        return employees;
    }


    private Employee parseEmployeeDetails(Element row) {
        Elements columns = row.select("td");

        // Extract name (exclude <span> content)
        String name = "N/A";
        Element employeeColumn = columns.select(".dailySchedule.employee").first();
        if (employeeColumn != null) {
            name = employeeColumn.ownText();
        }

        // Extract employee phone number (from span.empComments)
        String employeePhone = "N/A";
        if (employeeColumn != null) {
            Element phoneElement = employeeColumn.select("span.empComments").first();
            if (phoneElement != null) {
                employeePhone = phoneElement.text();
            }
        }

        // Extract shift
        String shift = columns.select(".dailySchedule.shift").text();

        // Extract job (exclude "Job Schedule" link text)
        String job = columns.select(".dailySchedule.job").html().split("<div")[0].trim();

        // Extract foreman name (exclude <span> content)
        String foreman = "N/A";
        Element foremanColumn = columns.select(".dailySchedule.foreman").first();
        if (foremanColumn != null) {
            foreman = foremanColumn.ownText();
        }

        // Extract foreman phone number (from span.noWrap.empComments)
        String foremanPhone = "N/A";
        if (foremanColumn != null) {
            Element foremanPhoneElement = foremanColumn.select("span.noWrap.empComments").first();
            if (foremanPhoneElement != null) {
                foremanPhone = foremanPhoneElement.text();
            }
        }

        // Extract crew
        String crew = columns.select(".dailySchedule.crew").text();

        Log.d("Employee", "Name: " + name + ", Shift: " + shift + ", Job: " + job + ", Foreman: " + foreman + ", Crew: " + crew + ", Employee Phone: " + employeePhone + ", Foreman Phone: " + foremanPhone);

        return new Employee(name, shift, job, foreman, crew, "N/A", employeePhone, foremanPhone);
    }

    /**
     * Callback interface for fetching the schedule.
     */
    public interface ScheduleCallback {
        void onSuccess(String scheduleDate, Employee mySchedule, List<Employee> employees);
        void onError(String error);
    }

}
