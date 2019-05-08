package com.csye6225.assignment1;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {

        public User findByEmail(String email);

}
