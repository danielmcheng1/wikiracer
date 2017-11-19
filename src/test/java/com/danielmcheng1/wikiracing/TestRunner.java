package com.danielmcheng1.wikiracing;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

public class TestRunner {
    public static void main(String[] args) {
        runOneSuite(WikiRetrieverTest.class, "unit tests for WikiRetriever");
        runOneSuite(CrawlTest.class, "unit tests for crawler");
        runOneSuite(CrawlEndToEndTest.class, "end to end tests for crawler");

    }

    private static void runOneSuite(Class c, String description) {
        printIntro(description);
        Result result = JUnitCore.runClasses(c);
        printFailures(result);
        printSummary(result, description);
    }

    private static void printIntro(String description) {
        System.out.println("-----------------------");
        System.out.println("Running " + description);
    }

    private static void printFailures(Result result) {
        for (Failure failure : result.getFailures()) {
            System.out.println(failure.toString());
        }
    }

    private static void printSummary(Result result, String description) {
        if (result.getFailureCount() == 0) {
            System.out.println("Successfully passed " + description);
        } else {
            System.out.println("Failed " + result.getFailureCount() + " tests in " + description);
        }
    }
}
