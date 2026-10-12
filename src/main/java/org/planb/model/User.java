package org.planb.model;

public class User {

    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private String passwordHash;

public User(Long id, String firstname, String lastname, String email, String passwordHash) {

    this.id = id;
    this.firstname = firstname;
    this.lastname = lastname;
    this.email = email;
    this.passwordHash = passwordHash;

    }
    public Long getId() {
        return id;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

}