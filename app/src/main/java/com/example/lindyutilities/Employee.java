package com.example.lindyutilities;

public class Employee {

    private String name;
    private String shift;
    private String job;
    private String foreman;
    private String crew;
    private String jobAddress;
    private String employeePhone;
    private String foremanPhone;

    public Employee(String name, String shift, String job, String foreman, String crew, String jobAddress, String employeePhone, String foremanPhone) {
        this.name = name;
        this.shift = shift;
        this.job = job;
        this.foreman = foreman;
        this.crew = crew;
        this.jobAddress = jobAddress;
        this.employeePhone = employeePhone;
        this.foremanPhone = foremanPhone;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getJob() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public String getForeman() {
        return foreman;
    }

    public void setForeman(String foreman) {
        this.foreman = foreman;
    }

    public String getCrew() {
        return crew;
    }

    public void setCrew(String crew) {
        this.crew = crew;
    }

    public String getJobAddress() {
        return jobAddress;
    }

    public void setJobAddress(String jobAddress) {
        this.jobAddress = jobAddress;
    }

    public String getEmployeePhone() {
        return employeePhone;
    }

    public void setEmployeePhone(String employeePhone) {
        this.employeePhone = employeePhone;
    }

    public String getForemanPhone() {
        return foremanPhone;
    }

    public void setForemanPhone(String foremanPhone) {
        this.foremanPhone = foremanPhone;
    }

    @Override
    public String toString() {
        return "Employee: " + name + "\n" +
                "Shift: " + shift + "\n" +
                "Job: " + job + "\n" +
                "Foreman: " + foreman + "\n" +
                "Crew: " + crew + "\n" +
                "Job Address: " + jobAddress + "\n" +
                "Employee Phone: " + employeePhone + "\n" +
                "Foreman Phone: " + foremanPhone + "\n";
    }
}
