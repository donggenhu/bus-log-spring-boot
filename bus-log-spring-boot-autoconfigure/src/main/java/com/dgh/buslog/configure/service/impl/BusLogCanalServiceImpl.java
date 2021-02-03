package com.dgh.buslog.configure.service.impl;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.Message;
import com.dgh.buslog.configure.config.BusLogProperties;
import com.dgh.buslog.configure.service.BusLogCanalService;
import com.dgh.buslog.configure.service.BusLogResolveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static java.lang.Thread.sleep;
import static java.util.concurrent.CompletableFuture.runAsync;

/**
 * @Author tiger
 * @Date 2021/2/1 14:19
 */
public class BusLogCanalServiceImpl implements BusLogCanalService {
    private Logger log = LoggerFactory.getLogger(BusLogCanalServiceImpl.class);

    private BusLogProperties properties;

    private BusLogResolveService busLogResolveService;

    public BusLogCanalServiceImpl(BusLogProperties properties, BusLogResolveService busLogResolveService) {
        this.properties = properties;
        this.busLogResolveService = busLogResolveService;
    }

    @Override
    public void canalStart() {
        //获取 CanalConnector
        CanalConnector connector = this.getCanalConnector();
        log.info("canalConnector 初始化完成 ...");
        for (; ; ) {
            Message message = connector.getWithoutAck(100);
            long batchId = message.getId();
            int size = message.getEntries().size();
            //存在数据，则解析
            if (batchId != -1 && size > 0) {
                runAsync(() -> busLogResolveService.resolving(message));
            } else {
                //未解析到数据， 则线程睡眠一段时间
                if (properties.getWaiteMillis() <= 0) {
                    continue;
                }
                try {
                    sleep(properties.getWaiteMillis());
                } catch (InterruptedException e) {
                    log.error("canal 同步线程暂停出错 ", e);
                }
            }
        }
    }


    /**
     * 获取 CanalConnector
     *
     * @return
     */
    private CanalConnector getCanalConnector() {
        CanalConnector canalConnector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(properties.getAddress(), properties.getPort()),
                properties.getDestination(), properties.getUsername(), properties.getPassword());
        canalConnector.connect();
        canalConnector.subscribe(".*\\..*");
        canalConnector.rollback();
        return canalConnector;
    }
}
