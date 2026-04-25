package org.example.quizizz;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuizizzApplication {

    private static final String APPLICATION_TIME_ZONE = "Asia/Ho_Chi_Minh";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(APPLICATION_TIME_ZONE));
        System.setProperty("user.timezone", APPLICATION_TIME_ZONE);
        SpringApplication.run(QuizizzApplication.class, args);
    }

}
