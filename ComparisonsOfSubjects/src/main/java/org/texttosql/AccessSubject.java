package org.texttosql;

import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.Immutable;

/**
 * Класс, представляющий субъекта доступа (сотрудника),
 * содержит поля для ФИО, email, телефона и должности
 */
@Immutable
public class AccessSubject {
    private final String lastName;
    private final String firstName;
    private final String middleName;
    private final String email;
    private final String phone;
    private final String position;

    public AccessSubject(String lastName, String firstName,
                         @Nullable String middleName, @Nullable String email,
                         @Nullable String phone, @Nullable String position) {
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.email = email;
        this.phone = phone;
        this.position = position;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public @Nullable String getMiddleName() {
        return middleName;
    }

    public @Nullable String getEmail() {
        return email;
    }

    public @Nullable String getPhone() {
        return phone;
    }

    public @Nullable String getPosition() {
        return position;
    }
}
