package com.csye6225.assignment1;

import org.springframework.data.repository.CrudRepository;

public interface NoteRepository extends CrudRepository<Note, Integer> {

    public Note findById(String Id);
}
