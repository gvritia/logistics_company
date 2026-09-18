package com.lcorp.console.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Entity связывает Java-класс с таблицей, а Column — поле со столбцом
@Entity
@Table(name = "clients", schema = "public")
public class Client {
    // Значение ID создаёт PostgreSQL своим identity-счётчиком
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;
    @Column(name = "phone", length = 32)
    private String phone;
    @Column(name = "email", length = 254)
    private String email;

    // Hibernate сначала создаёт пустой объект, затем заполняет поля из БД
    protected Client() {
    }

    public Client(String fullName, String phone, String email) {
        this(null, fullName, phone, email);
    }

    public Client(Long id, String fullName, String phone, String email) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Client{" +
            "id=" + id +
            ", fullName='" + fullName + '\'' +
            ", phone='" + phone + '\'' +
            ", email='" + email + '\'' +
            '}';
    }
}
