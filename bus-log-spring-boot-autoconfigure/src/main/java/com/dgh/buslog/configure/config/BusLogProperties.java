package com.dgh.buslog.configure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务日志配置
 * @Author tiger
 * @Date 2021/2/1 13:57
 */
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "du.basic.bus-log")
public class BusLogProperties {
    /**
     * 默认开启
     */
    private boolean enable = true;

    /**
     * canal server 服务地址
     */
    private String address = "127.0.0.1";
    /**
     * canal server 服务端口
     */
    private Integer port = 11111;
    /**
     * 一个canal server 可以运行多个 canal 实例， 指定一个特定的队列
     */
    private String destination = "destination";

    private String username = "";
    private String password = "";

    /**
     * 当无新的binlog解析的时候，线程暂停的时间，单位毫秒
     */
    private Integer waiteMillis = 300;

    /**
     * 保存操作日志的数据表名称
     */
    private String busLogTable = "du_bus_log";

    private List<String> executions = new ArrayList<>();

    public List<String> getExecutions() {
        return executions;
    }

    public void setExecutions(List<String> executions) {
        this.executions = executions;
    }

    public boolean getEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getWaiteMillis() {
        return waiteMillis;
    }

    public void setWaiteMillis(Integer waiteMillis) {
        this.waiteMillis = waiteMillis;
    }

    public String getBusLogTable() {
        return busLogTable;
    }

    public void setBusLogTable(String busLogTable) {
        this.busLogTable = busLogTable;
    }
}
