package com.dgh.buslog.configure.service;

import com.alibaba.otter.canal.protocol.Message;

/**
 * @Author tiger
 * @Date 2021/2/1 14:39
 */
public interface BusLogResolveService {
    /**
     * 解析数据
     * @param message canal 数据载体
     */
    void resolving(Message message);
}
