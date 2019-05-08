package com.csye6225.assignment1;

import javax.persistence.*;
import java.util.Set;

@Entity
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Integer id;

    private String pwd;

    private String email;

    @OneToMany(mappedBy = "user")
    private Set<Note> notes;


    public Set<Note> getLstNote() {
        return notes;
    }

    public void setLstNote(Set<Note> lstNote) {
        this.notes = lstNote;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getpwd() {
        return pwd;
    }

    public void setpwd(String pwd) {
        this.pwd = pwd;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


}
