package io.summerframework.sample;

import io.summerframework.core.boot.SummerApplication;

public class Application {

    public static void main(String[] args) throws InterruptedException {
        SummerApplication.run(AppConfig.class, args);
    }
}
