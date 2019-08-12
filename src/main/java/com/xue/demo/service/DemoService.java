package com.xue.demo.service;

import com.xue.mvcframework.annotation.XHJAutowired;
import com.xue.mvcframework.annotation.XHJService;

@XHJService
public class DemoService implements IDemoService {

    @XHJAutowired
    IDemoService demoService;

    public String get(String name) {
        return name;
    }
}
