package com.dgh.buslog.configure.configure;

import com.dgh.buslog.configure.config.BusLogProperties;
import com.dgh.buslog.configure.service.BusLogCanalService;
import com.dgh.buslog.configure.service.BusLogResolveService;
import com.dgh.buslog.configure.service.impl.BusLogCanalServiceImpl;
import com.dgh.buslog.configure.service.impl.BusLogResolveServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @Author tiger
 * @Date 2021/2/1 14:06
 */
@Configuration
@ConditionalOnProperty(value = "du.basic.bus-log.enable", havingValue = "true")
@EnableConfigurationProperties(BusLogProperties.class)
public class BugLogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BusLogResolveService getBusLogResolveService(BusLogProperties properties, JdbcTemplate jdbcTemplate) {
        return new BusLogResolveServiceImpl(jdbcTemplate, properties.getExecutions(), properties.getBusLogTable());
    }

    @Bean
    @ConditionalOnMissingBean
    public BusLogCanalService getBusLogCanalService(BusLogProperties properties, BusLogResolveService busLogResolveService) {
        return new BusLogCanalServiceImpl(properties, busLogResolveService);
    }
}
