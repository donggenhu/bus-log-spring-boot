package com.dgh.buslog.sample.config;

import com.dgh.buslog.configure.service.BusLogCanalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @Author tiger
 * @Date 2021/2/1 15:57
 */
@Component
public class InitConfig implements CommandLineRunner {
    @Autowired
    private BusLogCanalService busLogCanalService;

    @Override
    public void run(String... args) throws Exception {
        busLogCanalService.canalStart();
    }
}
