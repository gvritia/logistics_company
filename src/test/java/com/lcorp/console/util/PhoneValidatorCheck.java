package com.lcorp.console.util;

public final class PhoneValidatorCheck {

    public static void main(String[] args) {
        require(PhoneValidator.isValid("+79999999999"));
        require(PhoneValidator.isValid("+7-900-100-10-01"));
        reject(null);
        reject("");
        reject("   ");
        reject("набор строк");
        reject("79999999999");
        reject("+7999999999");
        reject("+799999999999");
        reject("+7-900-100-10-0x");
        reject("+79999999999abc");
        System.out.println("PASS: phone formats");
    }

    private static void reject(String value) {
        require(!PhoneValidator.isValid(value));
    }

    private static void require(boolean condition) {
        if (!condition) {
            throw new AssertionError("Некорректная проверка формата телефона");
        }
    }
}
