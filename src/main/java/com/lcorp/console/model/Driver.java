package com.lcorp.console.model;

public class Driver {
    private Long id;
    private String fullName;
    private String phone;
    private boolean active;

    public Driver(String fullName, String phone) {
        this(null, fullName, phone, true);
    }

    public Driver(Long id, String fullName, String phone, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "Driver{" +
            "id=" + id +
            ", fullName='" + fullName + '\'' +
            ", phone='" + phone + '\'' +
            ", active=" + active +
            '}';
    }
}
