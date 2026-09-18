package com.lcorp.console.app;

public enum Role {
    OPERATOR("Оператор"),
    DRIVER("Доставщик");

    private final String title;

    Role(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
