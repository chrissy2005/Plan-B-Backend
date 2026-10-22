package org.planb.dto;

public class UserResponse {
    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String name;
    private final String email;

    public UserResponse(Long id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = (firstName + " " + lastName).trim();
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
