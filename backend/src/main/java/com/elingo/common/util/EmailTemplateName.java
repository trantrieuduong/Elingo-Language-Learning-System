package com.elingo.common.util;

import lombok.Getter;

@Getter
public enum EmailTemplateName {
    SEND_OTP("send_otp");

    private final String name;
    
    EmailTemplateName(String name){
        this.name = name;
    }
}