package com.shopmall.config;

import com.shopmall.entity.SysAdmin;
import com.shopmall.mapper.SysAdminMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {
    private final SysAdminMapper adminMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String nickname;

    public BootstrapAdminInitializer(
            SysAdminMapper adminMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.username}") String username,
            @Value("${app.bootstrap-admin.password}") String password,
            @Value("${app.bootstrap-admin.nickname}") String nickname
    ) {
        this.adminMapper = adminMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.length() < 8) {
            throw new IllegalStateException("启用初始管理员时，ADMIN_INITIAL_PASSWORD 至少 8 位");
        }

        SysAdmin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            admin = new SysAdmin();
            admin.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
            admin.setUsername(username);
            admin.setPassword(passwordEncoder.encode(password));
            admin.setNickname(nickname);
            admin.setStatus(1);
            admin.setPwdResetRequired(0);
            adminMapper.insert(admin);
        }

        if (!adminMapper.selectRoleCodes(admin.getId()).contains("SUPER_ADMIN")) {
            adminMapper.insertSuperAdminRole(admin.getId());
        }
        if (!adminMapper.selectRoleCodes(admin.getId()).contains("SUPER_ADMIN")) {
            throw new IllegalStateException("缺少内置 SUPER_ADMIN 角色，请检查 Flyway 迁移");
        }
        log.info("初始管理员已就绪，username={}", username);
    }
}
