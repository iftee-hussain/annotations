package com.example.annotation;

import com.example.annotation.processor.Prototype;
import com.example.annotation.processor.Singletone;
import com.example.annotation.processor.Wired;

/**
 * Created by iftee on 10/19/18.
 */
@Singletone
public class TestService4 {
    @Wired
    TestService2 testService2;

    @Prototype
    TestService3 testService3;

    public void doSomething(){

    }
}
