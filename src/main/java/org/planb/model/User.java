package org.planb.model;

public class User {

    private long id;
    private String firstname;
    private String lastname;
    private String email;
    private String password;

public User(Long id, String firstname, String lastname, String email, String password) {

    this.id = id;
    this.firstname = firstname;
    this.lastname = lastname;
    this.email = email;
    this.password = password;

    }
    public long getId() {
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

    public String getPassword() {
        return password;
    }

}