package com.github.ffq.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @Author: ffq
 * @Description:
 * @Date: Create in 11:11 2019/1/5
 */
public class MyInvocationHandler implements InvocationHandler {
    //增强的目标类
    private Person person;

    public MyInvocationHandler(Person person) {
        this.person = person;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("先吃饭-----再看书");
        method.invoke(person, args);
        return null;
    }
}
