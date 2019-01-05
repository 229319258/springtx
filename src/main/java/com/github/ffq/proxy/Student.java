package com.github.ffq.proxy;

/**
 * @Author: ffq
 * @Description:
 * @Date: Create in 11:09 2019/1/5
 */
public class Student implements Person {
    @Override
    public void work() {
        System.out.println("读书");
        try {
            this.work2();
        } catch (Exception e) {

        }
    }


    public void work2() {
        System.out.println("不想读啊");
        int i = 1 / 0;
    }

}
