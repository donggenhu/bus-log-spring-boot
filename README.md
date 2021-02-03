# 基于canal记录业务日志,增量的方式

构建一个用于支撑业务数据变更的starter

1. 添加maven依赖dependency
2. 启动项目后，框架会自动创建对应的数据表（配置文件 busLogTable）
3. 数据源配置好，框架会依赖实例化JdbcTemplate
4. 配置canal实例
5. 配置需要解析的表，以及对应增删改需要记录的日志格式

使用的框架如下：

* JdbcTemplate
* commons-text
* druid-spring-boot-starter

# Getting Started

maven
```xml
<dependency>
    <groupId>com.dgh</groupId>
    <artifactId>bus-log-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

application.yml
```yml

spring:
  datasource:
    password: retl
    username: retl
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:mysql://10.176.76.16:13306/retl?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull&useSSL=false
du:
  basic:
    bus-log:
      enable: true
      address: 10.176.76.16
      port: 11111
      destination: canal-test
      waiteMillis: 300
      busLogTable: bus_log_test
      executions:
        - '{"schema":"retl","table":"xdual","add":"测试业务 新增数据 时间: ${X}, ID: ${ID}","edit":"测试业务 修改数据 时间: [${X_OLD}] 修改为 [${X_NEW}], ID: ${ID_NEW}","delete":"测试业务 删除数据 时间: ${X}, ID: ${ID}"}'
        - '{"schema":"retl","table":"xdual2","add":"测试业务2 新增数据 时间: ${X}, ID: ${ID}","edit":"测试业务2 修改数据 时间: [${X_OLD}] 修改为 [${X_NEW}], ID: ${ID_NEW}","delete":"测试业务2 删除数据 时间: ${X}, ID: ${ID}"}'

```
* executions 节点表示的需要解析的库表以及具体的业务形式

> add, edit , delete 分别表示 数据在 insert, update , delete 的时候生成的日志格式;
> 
> 字段名称需要大写，对于update的数据有新旧之分，所以需要添加后缀 _OLD, _NEW

code
```java
/**
 * 启动SpringBoot项目完成之后自动执行
 */
@Component
public class InitConfig implements CommandLineRunner {
    // 注入业务日志记录的 Service
    @Autowired
    private BusLogCanalService busLogCanalService;

    @Override
    public void run(String... args) throws Exception {
        //调用 canalStart方法，处理binlog
        busLogCanalService.canalStart();
    }
}

```

最终产生的效果
```text
SELECT * from bus_log_test;

id  table_name type content create_time modify_time
7	xdual2	UPDATE	测试业务2 修改数据 时间: [2021-02-02 21:53:41] 修改为 [2021-02-02 21:53:44], ID: 112	2021-02-02 13:53:44	1971-01-01 00:00:00
8	xdual2	DELETE	测试业务2 删除数据 时间: 2021-02-02 21:53:44, ID: 112	2021-02-02 13:53:44	1971-01-01 00:00:00
9	xdual2	INSERT	测试业务2 新增数据 时间: 2021-02-02 21:53:47, ID: 112	2021-02-02 13:53:47	1971-01-01 00:00:00
10	xdual2	DELETE	测试业务2 删除数据 时间: 2021-02-02 21:53:47, ID: 112	2021-02-02 13:53:47	1971-01-01 00:00:00
11	xdual	INSERT	测试业务 新增数据 时间: 2021-02-02 21:53:59, ID: 112	2021-02-02 13:53:59	1971-01-01 00:00:00
12	xdual	DELETE	测试业务 删除数据 时间: 2021-02-02 21:53:59, ID: 112	2021-02-02 13:53:59	1971-01-01 00:00:00
```


