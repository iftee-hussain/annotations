package com.example.annotation;

import com.example.annotation.processor.Singletone;
import com.example.annotation.processor.Wired;


@Singletone
public class TestService1 {
	@Wired
	TestService2 testService2;
	
	@Wired
	TestService3 testService3;
	
	public void doSomething() {

	}

}
