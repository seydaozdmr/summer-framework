package io.summerframework.example;

import io.summerframework.core.context.AnnotationApplicationContext;

public class Main {

    public static void main(String[] args) {
        try (AnnotationApplicationContext context = new AnnotationApplicationContext(AppConfig.class)) {
            UserFacade facade = context.getBean(UserFacade.class);
            System.out.println(facade.welcome("Seyda"));
        }
    }
}
