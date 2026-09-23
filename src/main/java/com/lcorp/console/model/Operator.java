package com.lcorp.console.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Entity связывает Java-класс с таблицей, а Column — поле со столбцом
@Entity
@Table(name = "operators", schema = "public")
public class Operator {
    // Значение ID создаёт PostgreSQL своим identity-счётчиком
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;
    @Column(name = "phone", length = 32, nullable = false)
    private String phone;

    protected Operator() {
    }

    public Operator(String fullName, String phone) {
        this(null, fullName, phone);
    }

    public Operator(Long id, String fullName, String phone){
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public String getFullName(){
        return fullName;
    }

    public void setFullName(String fullName){
        this.fullName = fullName;
    }

    public String getPhone(){
        return phone;
    }

    public void setPhone(String phone){
        this.phone = phone;
    }

    @Override
    public String toString(){
        return "Operator{" +
            "id=" + id +
            ", fullName='" + fullName + '\'' +
            ", phone='" + phone + '\'' +
            '}';
    }
}
