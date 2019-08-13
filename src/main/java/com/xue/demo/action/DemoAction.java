package com.xue.demo.action;

import com.xue.demo.service.DemoService;
import com.xue.demo.service.IDemoService;
import com.xue.mvcframework.annotation.XHJAutowired;
import com.xue.mvcframework.annotation.XHJController;
import com.xue.mvcframework.annotation.XHJRequestMapping;
import com.xue.mvcframework.annotation.XHJRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@XHJController
@XHJRequestMapping("/demo")
public class DemoAction {

    private DemoService demoService;

    @XHJRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @XHJRequestParam("name") String name){

        String result = "my name is "+name;
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
