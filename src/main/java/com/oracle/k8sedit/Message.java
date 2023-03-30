
package com.oracle.k8sedit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class Message {

    private String message;

    private String greeting;

    public Message() {
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }

    @JsonInclude(Include.NON_NULL)
    public String getGreeting() {
        return this.greeting;
    }
}
