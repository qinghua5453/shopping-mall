package com.shopmall.bootstrap.config;

import com.shopmall.security.application.service.AdminAccountService;
import com.shopmall.security.config.BootstrapAdminProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final AdminAccountService accountService;
    private final BootstrapAdminProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (properties.password() == null || properties.password().length() < 8) {
            throw new IllegalStateException("启用初始管理员时，ADMIN_INITIAL_PASSWORD 至少 8 位");
        }
        accountService.createBootstrapAdmin(
                properties.username(), properties.password(), properties.nickname());
        log.info("初始管理员已就绪，username={}，请使用环境变量配置的密码登录", properties.username());
    }
}
