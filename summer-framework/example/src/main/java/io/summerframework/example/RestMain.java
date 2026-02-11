package io.summerframework.example;

import io.summerframework.core.boot.SummerApplication;

public class RestMain {

    public static void main(String[] args) throws InterruptedException {
        SummerApplication.run(AppConfig.class, args);
    }
}
