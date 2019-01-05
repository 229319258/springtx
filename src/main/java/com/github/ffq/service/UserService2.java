package com.github.ffq.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: ffq
 * @Description:
 * @Date: Create in 9:42 2019/1/5
 */
@Service
@Slf4j
public class UserService2 {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save2() {
        jdbcTemplate.execute("INSERT INTO user (id, name) VALUES\n" +
                "(6, 'Jack6')");
        int i = 1 / 0;
    }
}
