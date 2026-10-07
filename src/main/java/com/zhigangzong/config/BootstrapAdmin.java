package com.zhigangzong.config;

import com.zhigangzong.entity.School;
import com.zhigangzong.entity.UserAccount;
import com.zhigangzong.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Explicit, local first-run setup. Existing credentials are never replaced. */
@Component
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class BootstrapAdmin implements ApplicationRunner {
    private final BootstrapMapper bootstrap;
    private final SchoolMapper schools;
    private final UserAccountMapper users;
    private final PasswordEncoder encoder;
    @Value("${app.bootstrap.password:}") private String password;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrap.accountCount() > 0) return;
        if (password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("首次管理员密码需为至少 12 字符、最多 72 UTF-8 字节，请配置 app.bootstrap.password");
        }
        Long schoolId = bootstrap.schoolId();
        if (schoolId == null) {
            School school = new School();
            school.setName("本地管理学校");
            school.setCode("ZGZ-LOCAL");
            schools.insert(school);
            schoolId = school.getId();
        }
        UserAccount user = new UserAccount();
        user.setSchoolId(schoolId);
        user.setDisplayName("本地管理员");
        user.setRole("SCHOOL_ADMIN");
        users.insert(user);
        bootstrap.insertAccount(user.getId(), encoder.encode(password));
    }
}
