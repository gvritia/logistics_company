package com.lcorp.console.model;

import jakarta.persistence.*;

@Entity
@Table(name = "clients", schema = "public")
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;
    @Column(name = "phone", length = 32)
    private String phone;
    @Column(name = "email", length = 254)
    private String email;


    protected Client() {
    }

    public Client(
        String fullName,
        String phone,
        String email
    ) {
        this(null, fullName, phone, email);
    }

    public Client(
        Long id,
        String fullName,
        String phone,
        String email
    ) {
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
