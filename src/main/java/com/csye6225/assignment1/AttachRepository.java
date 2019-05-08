package com.csye6225.assignment1;

import org.springframework.data.repository.CrudRepository;

public interface AttachRepository extends CrudRepository<Attachment, Integer> {

    public Attachment findById(String Id);
}