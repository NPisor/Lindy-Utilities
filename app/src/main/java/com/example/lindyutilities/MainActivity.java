package com.example.lindyutilities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LindyUtilitiesPrefs";
    private static final String EMPLOYEE_ID_KEY = "employeeId";

    TextView tvMySchedule, tvMyShift, tvMyJob, tvMyForeman, tvMyAddress, tvMyForemanPhone;
    String employeeId;
    Button sortButton, btnGetDirections;
    EmployeeAdapter adapter;
    private List<Employee> originalEmployeeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvMySchedule = findViewById(R.id.tvMySchedule);
        tvMyShift = findViewById(R.id.tvMyShift);
        tvMyJob = findViewById(R.id.tvMyJob);
        tvMyForeman = findViewById(R.id.tvMyForeman);
        tvMyAddress = findViewById(R.id.tvMyJobAddress);
        tvMyForemanPhone = findViewById(R.id.tvMyForemanPhone);
        btnGetDirections = findViewById(R.id.btnGetDirections);

        sortButton = findViewById(R.id.sortButton);

        employeeId = getCachedEmployeeId();
        if (employeeId == null) {
            promptForEmployeeId();
            return;
        }

        RecyclerView recyclerView = findViewById(R.id.rvEmployees);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100);
            }
        }
        requestBatteryOptimizationException();

        String workerName = employeeId;

        // Cancel any existing workers with the same name
        WorkManager.getInstance(this).cancelUniqueWork(workerName);

        PeriodicWorkRequest scheduleWorkRequest = new PeriodicWorkRequest.Builder(
                ScheduleWorker.class,
                10, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                workerName,
                ExistingPeriodicWorkPolicy.REPLACE,
                scheduleWorkRequest
        );

        // Fetch the schedule
        DailyScheduleHelper dailyScheduleHelper = new DailyScheduleHelper(employeeId, this);
        dailyScheduleHelper.fetchSchedule(new DailyScheduleHelper.ScheduleCallback() {
            @Override
            public void onSuccess(String scheduleDate, Employee mySchedule, List<Employee> employees) {
                runOnUiThread(() -> {
                    if (mySchedule != null) {
                        tvMySchedule.setText("My Schedule for " + scheduleDate);
                        tvMyShift.setText("Shift: " + mySchedule.getShift());
                        tvMyJob.setText("Job: " + mySchedule.getJob());
                        tvMyForeman.setText("Foreman: " + mySchedule.getForeman());
                        tvMyForemanPhone.setText("Foreman Phone: " + mySchedule.getForemanPhone());
                        tvMyAddress.setText("Job Address: " + mySchedule.getJobAddress());
                        btnGetDirections.setVisibility(View.VISIBLE);
                    }
                    btnGetDirections.setEnabled(!mySchedule.getJobAddress().equals("N/A"));
                    btnGetDirections.setOnClickListener(v -> openGoogleMapsWithAddress(mySchedule.getJobAddress()));
                    originalEmployeeList = employees;
                    adapter = new EmployeeAdapter(employees);
                    recyclerView.setAdapter(adapter);

                    searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                        @Override
                        public boolean onQueryTextSubmit(String query) {
                            adapter.filter(query);
                            return false;
                        }

                        @Override
                        public boolean onQueryTextChange(String newText) {
                            adapter.filter(newText);
                            return false;
                        }
                    });

                    sortButton.setOnClickListener(v -> showSortDialog(employees));
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> tvMySchedule.setText("Error: Unable to load schedule"));
            }
        });
    }

    private void showSortDialog(List<Employee> employees) {
        // Extract unique job sites from the original list
        Set<String> jobSites = new HashSet<>();
        for (Employee employee : originalEmployeeList) {
            jobSites.add(employee.getJob());
        }

        // Convert to array for the dialog
        String[] jobArray = jobSites.toArray(new String[0]);
        boolean[] checkedJobs = new boolean[jobArray.length];
        List<String> selectedJobs = new ArrayList<>();

        // Create and show the main job dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Job Sites");

        builder.setMultiChoiceItems(jobArray, checkedJobs, (dialog, which, isChecked) -> {
            if (isChecked) {
                selectedJobs.add(jobArray[which]);
            } else {
                selectedJobs.remove(jobArray[which]);
            }
        });

        builder.setPositiveButton("Next", (dialog, which) -> {
            if (!selectedJobs.isEmpty()) {
                showCrewDialog(selectedJobs);
            } else {
                Toast.makeText(this, "Please select at least one job site", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNeutralButton("All", (dialog, which) -> {
            adapter.updateList(originalEmployeeList, new ArrayList<>(originalEmployeeList));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCrewDialog(List<String> selectedJobs) {
        // Extract unique crews for the selected jobs
        Set<String> crews = new HashSet<>();
        for (Employee employee : originalEmployeeList) {
            if (selectedJobs.contains(employee.getJob())) {
                crews.add(employee.getCrew());
            }
        }

        // Convert to array for the dialog
        String[] crewArray = crews.toArray(new String[0]);
        boolean[] checkedCrews = new boolean[crewArray.length];
        List<String> selectedCrews = new ArrayList<>();

        // Create and show the crew dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Crews");

        builder.setMultiChoiceItems(crewArray, checkedCrews, (dialog, which, isChecked) -> {
            if (isChecked) {
                selectedCrews.add(crewArray[which]);
            } else {
                selectedCrews.remove(crewArray[which]);
            }
        });

        builder.setPositiveButton("Apply", (dialog, which) -> {
            // Filter employees based on selected jobs and crews
            List<Employee> filteredList = new ArrayList<>();
            for (Employee employee : originalEmployeeList) {
                if (selectedJobs.contains(employee.getJob()) && selectedCrews.contains(employee.getCrew())) {
                    filteredList.add(employee);
                }
            }

            // Update the adapter with the filtered list
            adapter.updateList(originalEmployeeList, filteredList);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String getCachedEmployeeId() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getString(EMPLOYEE_ID_KEY, null);
    }

    private void saveEmployeeId(String employeeId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putString(EMPLOYEE_ID_KEY, employeeId).apply();
    }

    private void promptForEmployeeId() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Your Employee ID");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String enteredId = input.getText().toString().trim();
            if (!enteredId.isEmpty()) {
                saveEmployeeId(enteredId);
                employeeId = enteredId;
                recreate(); // Restart the activity to use the new employee ID
            } else {
                Toast.makeText(this, "Employee ID cannot be empty.", Toast.LENGTH_SHORT).show();
                promptForEmployeeId(); // Retry if empty
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WorkManager.getInstance(this).cancelAllWork();
    }

    public void openGoogleMapsWithAddress(String address) {
        new Thread(() -> {
            try {
                Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(address));
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

                // Do NOT set package - allow user to choose app
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(mapIntent, "Choose a maps app"));
                } else {
                    // Open in browser as a fallback
                    Uri webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(address));
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
                    startActivity(webIntent);
                }
            } catch (Exception e) {
                Log.e("MapsIntentError", "Failed to launch Maps Intent", e);
            }
        }).start(); // Run in background thread
    }

    public void requestBatteryOptimizationException() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }
}
