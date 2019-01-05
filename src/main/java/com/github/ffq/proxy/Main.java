package com.github.ffq.proxy;

import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * @Author: ffq
 * @Description:
 * @Date: Create in 11:15 2019/1/5
 */
public class Main {
    public static void main(String[] args) {
        Person person = new Student();
        MyInvocationHandler myInvocationHandler = new MyInvocationHandler(person);
        System.out.println(Arrays.toString(Student.class.getInterfaces()));
        Person proPerson = (Person) Proxy.newProxyInstance(Student.class.getClassLoader(), Student.class.getInterfaces(), myInvocationHandler);
        proPerson.work();
    }
}
