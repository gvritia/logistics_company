package com.lcorp.javafx.model;

public record ReferenceOption(Long id, String name) {
    @Override
    public String toString() {
        return name + " (№ " + id + ")";
    }
}
