package com.xue.mvcframework.servlet;

import com.xue.mvcframework.annotation.XHJAutowired;
import com.xue.mvcframework.annotation.XHJController;
import com.xue.mvcframework.annotation.XHJRequestMapping;
import com.xue.mvcframework.annotation.XHJService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class XHJDispatcherServlet extends HttpServlet {

    private static final long sericalVersionUID = 1L;

    //和web.xml中param-name的值一致
    private static final  String LOCATION = "contextConfigLocation";

    //保存所有的配置信息
    private Properties p = new Properties();

    //保存所有被扫描到的相关的类名
    private List<String> classNames = new ArrayList<String>();

    //核心IOC容器，保存所有初始化的Bean
    private Map<String,Object> ioc = new HashMap<String, Object>();

    //保存所有的Url和方法的映射关系
    private Map<String,Method> handlerMapping = new HashMap<String, Method>();

    public XHJDispatcherServlet(){super();}

    /**
     * 初始化，加载配置文件
     */
    public void init(ServletConfig config) throws ServletException{
        //1、加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));
        //2、扫描所有的相关类
        doScanner(p.getProperty("scanPackage"));
        //3、初始化所有相关类的实例，并保存到IOC容器中
        doInstance();
        //4、依赖注入
        doAutowired();
        //5、构造HandlerMapping
        initHandlerMapping();
        //6、等待请求，匹配URL，定位方法，反射调用执行
        //调用doGet或doPost方法

        //提示信息
        System.out.println("xhj mvcfarmework is init");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()){return;}

        for (Map.Entry<String,Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(XHJController.class)){continue;}

            String baseUrl = "";
            //获取controller的url配置
            if (clazz.isAnnotationPresent(XHJRequestMapping.class)){
                XHJRequestMapping requestMapping = clazz.getAnnotation(XHJRequestMapping.class);
                baseUrl = requestMapping.value();
            }
            //获取method的url配置
            Method[] methods = clazz.getMethods();
            for (Method method:
                 methods) {
                if (!method.isAnnotationPresent(XHJRequestMapping.class)){continue;}

                //映射URL
                XHJRequestMapping requestMapping = method.getAnnotation(XHJRequestMapping.class);
                String url = ("/"+baseUrl+"/"+requestMapping.value()).replaceAll("/+","/");
                handlerMapping.put(url,method);
                System.out.println("mapped "+url+","+method);
            }
        }
    }

    private void doAutowired() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String,Object> entry :ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field:fields){
                if (!field.isAnnotationPresent(XHJAutowired.class)){continue;}

                XHJAutowired autowired = field.getAnnotation(XHJAutowired.class);
                String beanName = autowired.value().trim();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);//设置私有属性的访问权限
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }

    private String lowerFirstCase(String str){
        char[] chars = str.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }

    private void doInstance() {
        if (classNames.size() == 0){return;}
        try {
            for (String className:classNames){
                Class<?>  clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(XHJController.class)){
                    //默认将首字母小写作为beanName
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,clazz.newInstance());
                }else if (clazz.isAnnotationPresent(XHJService.class)){
                    XHJService service = clazz.getAnnotation(XHJService.class);
                    String beanName = service.value();
                    //如果用户设置了名字，就用用户自己的名字
                    if (!"".equals(beanName.trim())){
                        ioc.put(beanName,clazz.newInstance());
                        continue;
                    }
                    //如果没有设置，就按接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> cl: interfaces
                         ) {
                        ioc.put(cl.getName(),clazz.newInstance());
                    }
                }else{
                    continue;
                }
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void doScanner(String scanPackage) {
        //将所有的包路径转换为文件路径
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for (File file: dir.listFiles()) {
            //如果是文件夹，进行递归
            if (file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                classNames.add(scanPackage+"."+file.getName().replace(".class","").trim());
            }
        }
    }

    private void doLoadConfig(String localtion) {
        InputStream fis = null;

        fis = this.getClass().getClassLoader().getResourceAsStream(localtion);
        try {
            p.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null!=fis){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException,IOException{
        this.doPost(req,res);
    }
    /**
     * 执行业务处理
     */
    protected void doPost(HttpServletRequest req,HttpServletResponse res) throws ServletException,IOException{

        try {
            doDispatch(req,res);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse res) throws IOException, InvocationTargetException, IllegalAccessException {
        if (this.handlerMapping.isEmpty()){return;}

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath,"").replaceAll("/+","/");

        if (!this.handlerMapping.containsKey(url)){
            res.getWriter().write("404 NOT FOUND!");
            return;
        }

        Map<String,String[]> params = req.getParameterMap();
        Method method = this.handlerMapping.get(url);
        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(this.ioc.get(beanName),req,res,params.get("name")[0]);


    }
}
