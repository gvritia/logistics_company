package com.lcorp.console.util;

import java.util.regex.Pattern;

public final class PhoneValidator {

    // Компактный формат и формат уже существующих seed-номеров.
    private static final Pattern PHONE = Pattern.compile(
        "(?:\\+7[0-9]{10}|\\+7-[0-9]{3}-[0-9]{3}-[0-9]{2}-[0-9]{2})"
    );

    private PhoneValidator() {
    }

    public static boolean isValid(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }
}
