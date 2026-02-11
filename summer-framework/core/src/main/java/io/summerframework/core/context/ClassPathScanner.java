package io.summerframework.core.context;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

final class ClassPathScanner {

    Set<Class<?>> scan(String basePackage) {
        Set<Class<?>> classes = new HashSet<>();
        String path = basePackage.replace('.', '/');

        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (!"file".equals(resource.getProtocol())) {
                    continue;
                }
                String decodedPath = URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8);
                File directory = new File(decodedPath);
                collectClasses(directory, basePackage, classes);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan package: " + basePackage, ex);
        }

        return classes;
    }

    private void collectClasses(File directory, String packageName, Set<Class<?>> classes) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                collectClasses(file, packageName + "." + file.getName(), classes);
                continue;
            }

            if (!file.getName().endsWith(".class") || file.getName().contains("$")) {
                continue;
            }

            String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
            try {
                classes.add(Class.forName(className));
            } catch (ClassNotFoundException ex) {
                throw new IllegalStateException("Could not load class: " + className, ex);
            }
        }
    }
}
