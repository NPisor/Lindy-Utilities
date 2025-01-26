package com.example.lindyutilities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.EmployeeViewHolder> {

    private List<Employee> originalEmployees;
    private final List<Employee> filteredEmployees;
    private int expandedPosition = RecyclerView.NO_POSITION; // Track expanded item

    public EmployeeAdapter(List<Employee> employees) {
        this.originalEmployees = employees;
        this.filteredEmployees = new ArrayList<>(employees);
    }

    @NonNull
    @Override
    public EmployeeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false);
        return new EmployeeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EmployeeViewHolder holder, int position) {
        Employee employee = filteredEmployees.get(position);
        Context context = holder.itemView.getContext();

        // Bind employee data to the views
        holder.tvEmployeeName.setText("Name: " + employee.getName());
        holder.tvEmployeePhone.setText("Phone: " + employee.getEmployeePhone());
        holder.tvShift.setText("Shift: " + employee.getShift());
        holder.tvJob.setText("Job: " + employee.getJob());
        holder.tvForeman.setText("Foreman: " + employee.getForeman());
        holder.tvForemanPhone.setText("Foreman Phone: " + employee.getForemanPhone());
        holder.tvCrew.setText("Crew: " + employee.getCrew());
        holder.tvJobAddress.setText(employee.getJobAddress());

        // Manage expanded state
        boolean isExpanded = position == expandedPosition;
        holder.trayLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Set click listener for toggling the tray
        holder.itemView.setOnClickListener(v -> {
            int previousExpandedPosition = expandedPosition;
            expandedPosition = isExpanded ? RecyclerView.NO_POSITION : position;
            notifyItemChanged(previousExpandedPosition); // Collapse previous tray
            notifyItemChanged(expandedPosition); // Expand current tray
        });

        // Set Call button functionality
        holder.btnCall.setOnClickListener(v -> {
            if (!employee.getEmployeePhone().equals("N/A")) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + employee.getEmployeePhone()));
                context.startActivity(callIntent);
            } else {
                Toast.makeText(context, "No phone number available for " + employee.getName(), Toast.LENGTH_SHORT).show();
            }
        });

        // Set Message button functionality
        holder.btnMessage.setOnClickListener(v -> {
            if (!employee.getEmployeePhone().equals("N/A")) {
                Intent messageIntent = new Intent(Intent.ACTION_SENDTO);
                messageIntent.setData(Uri.parse("smsto:" + employee.getEmployeePhone()));
                context.startActivity(messageIntent);
            } else {
                Toast.makeText(context, "No phone number available for " + employee.getName(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredEmployees.size();
    }

    public void filter(String query) {
        filteredEmployees.clear();
        if (query.isEmpty()) {
            filteredEmployees.addAll(originalEmployees);
        } else {
            for (Employee employee : originalEmployees) {
                if (employee.getName().toLowerCase().contains(query.toLowerCase())) {
                    filteredEmployees.add(employee);
                }
            }
        }
        notifyDataSetChanged();
    }

    static class EmployeeViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmployeeName, tvEmployeePhone, tvShift, tvJob, tvForeman, tvForemanPhone, tvCrew, tvJobAddress;
        LinearLayout trayLayout;

        Button btnCall, btnMessage;

        public EmployeeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvEmployeePhone = itemView.findViewById(R.id.tvEmployeePhone);
            tvShift = itemView.findViewById(R.id.tvShift);
            tvJob = itemView.findViewById(R.id.tvJob);
            tvForeman = itemView.findViewById(R.id.tvForeman);
            tvForemanPhone = itemView.findViewById(R.id.tvForemanPhone);
            tvCrew = itemView.findViewById(R.id.tvCrew);
            tvJobAddress = itemView.findViewById(R.id.tvJobAddress);
            trayLayout = itemView.findViewById(R.id.trayLayout);
            btnCall = itemView.findViewById(R.id.btnCall);
            btnMessage = itemView.findViewById(R.id.btnMessage);
        }
    }

    public void updateList(List<Employee> originalList, List<Employee> filteredList) {
        // Update only the filteredEmployees list
        filteredEmployees.clear();
        filteredEmployees.addAll(filteredList);
        notifyDataSetChanged();
    }

}
