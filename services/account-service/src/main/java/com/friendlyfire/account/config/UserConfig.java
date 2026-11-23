package com.friendlyfire.account.config;

import com.friendlyfire.account.user.NewUserNames;
import java.security.SecureRandom;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Users: the names new users start with. */
@Configuration(proxyBeanMethods = false)
public class UserConfig {

    // SecureRandom is safe to share between threads.
    @Bean
    NewUserNames newUserNames() {
        return new NewUserNames(new SecureRandom());
    }
}
