package com.csye6225.assignment1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
public class Attachment {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name="UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(name="Aid",updatable = false,nullable = false)
    private String id;
    private String url;

    @ManyToOne
    @JoinColumn(name ="Noteid",nullable = false)
    private Note note;

    @JsonIgnore
    public Note getNote() { return note; }

    public void setNote(Note note) {
        this.note = note;
    }

    public String getId() {

        return id;
    }

    public void setId(String Id) {

        id = Id;
    }

    public String getUrl() {

        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }

}