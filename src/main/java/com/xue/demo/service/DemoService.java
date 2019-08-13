package com.xue.demo.service;

import com.xue.mvcframework.annotation.XHJAutowired;
import com.xue.mvcframework.annotation.XHJService;

@XHJService
public class DemoService {

    public String get(String name) {
        return name;
    }
}
