package com.lcorp.console.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Entity связывает Java-класс с таблицей, а Column — поле со столбцом
@Entity
@Table(name = "drivers", schema = "public")
public class Driver {
    // Значение ID создаёт PostgreSQL своим identity-счётчиком
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;
    @Column(name = "phone", length = 32, nullable = false)
    private String phone;
    @Column(name = "active", nullable = false)
    private boolean active;

    // Hibernate сначала создаёт пустой объект, затем заполняет поля из БД
    protected Driver() {
    }

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
