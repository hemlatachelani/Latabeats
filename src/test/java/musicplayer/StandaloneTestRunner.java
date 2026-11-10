package musicplayer;

import musicplayer.algo.AlgorithmsTest;
import musicplayer.ds.DataStructuresTest;
import musicplayer.service.SongGraphTest;

import java.lang.reflect.Method;

/**
 * Standalone console test runner.
 * Runs all JUnit test suites directly without needing external plugins.
 */
public class StandaloneTestRunner {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("          LataBeats - Data Structures & Algorithms Test Suite  ");
        System.out.println("===============================================================");

        int passed = 0;
        int failed = 0;

        Class<?>[] testClasses = {
                DataStructuresTest.class,
                AlgorithmsTest.class,
                SongGraphTest.class
        };

        for (Class<?> clazz : testClasses) {
            System.out.println("\nRunning suite: " + clazz.getSimpleName());
            System.out.println("---------------------------------------------------------------");

            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(org.junit.jupiter.api.Test.class)) {
                        String name = m.getName();
                        org.junit.jupiter.api.DisplayName dn = m.getAnnotation(org.junit.jupiter.api.DisplayName.class);
                        if (dn != null) name = dn.value();

                        try {
                            m.invoke(instance);
                            System.out.println("  [PASS] " + name);
                            passed++;
                        } catch (Exception ex) {
                            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                            System.err.println("  [FAIL] " + name + " -> " + cause.getMessage());
                            cause.printStackTrace();
                            failed++;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error instantiating test class " + clazz.getName() + ": " + e.getMessage());
                failed++;
            }
        }

        System.out.println("\n===============================================================");
        System.out.printf("Test Summary: %d PASSED, %d FAILED (Total: %d)%n", passed, failed, passed + failed);
        System.out.println("===============================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }
}
