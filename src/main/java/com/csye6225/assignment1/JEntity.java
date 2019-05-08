package com.csye6225.assignment1;

import org.springframework.http.HttpStatus;

public class JEntity {
    private String msg;
    private HttpStatus statuscode;
    private int code;

    public HttpStatus getStatuscode() {
        return statuscode;
    }

    public void setStatuscode(HttpStatus statuscode) {
        this.statuscode = statuscode;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}

