package com.example.lindyutilities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class ScheduleWorker extends Worker {

    private static final String CHANNEL_ID = "schedule_updates_channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "LindyUtilitiesPrefs";
    private static final String EMPLOYEE_ID_KEY = "employeeId";

    public ScheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        String employeeId = getCachedEmployeeId();
        if (employeeId == null || employeeId.isEmpty()) {
            Log.e("ScheduleWorker", "Employee ID is missing. Please set it before running the worker.");
            return Result.failure(); // Fail the work if employeeId is not available
        }

        DailyScheduleHelper dailyScheduleHelper = new DailyScheduleHelper(employeeId, getApplicationContext());

        dailyScheduleHelper.fetchSchedule(new DailyScheduleHelper.ScheduleCallback() {
            @Override
            public void onSuccess(String scheduleDate, Employee employee, List<Employee> employees) {
                String cachedDate = getCachedDate();
                if (!scheduleDate.equals(cachedDate)) {
                    updateCachedDate(scheduleDate); // Update the cached date
                    sendNotification("Schedule Updated", "Your schedule has been updated for " + scheduleDate);
                } else {
                    Log.d("ScheduleWorker", "No schedule changes detected.");
                }
            }

            @Override
            public void onError(String error) {
                Log.e("ScheduleWorker", "Failed to fetch schedule: " + error);
            }
        });

        return Result.success();
    }

    // Helper methods for caching the employee ID and date
    private String getCachedEmployeeId() {
        Context context = getApplicationContext();
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(EMPLOYEE_ID_KEY, null);
    }

    private String getCachedDate() {
        Context context = getApplicationContext();
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("cachedDate", "Not Found");
    }

    private void updateCachedDate(String date) {
        Context context = getApplicationContext();
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("cachedDate", date)
                .apply();
    }

    private void sendNotification(String title, String message) {
        Context context = getApplicationContext();
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Schedule Updates",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Create an Intent to open MainActivity
        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Create the PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0, // Request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Ensure the intent is immutable
        );

        // Build and send the notification
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss the notification when clicked
                .setContentIntent(pendingIntent) // Set the PendingIntent
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}
