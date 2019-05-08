package com.csye6225.assignment1;

import org.junit.Test;

import static org.junit.Assert.*;

public class MainControllerTest {

    MainController mainController = new MainController();
//    @Test
//    public void validateEmail() {
//        String email;
//        email = "abcdgmail.com";
//        //  boolean validEmail = mainController.validateEmail(email);
//
//        assertTrue("Correct Email Format",mainController.validateEmail(email));
//
//    }

    @Test
    public void validatePwd() {
        String password;
        password = "Abc@1234";

        System.out.println(mainController.validatePwd(password));

        //  boolean validEmail = mainController.validateEmail(email);
        assertEquals("Password shoud be 8 characters or above",true,mainController.validatePwd(password));

    }


}
