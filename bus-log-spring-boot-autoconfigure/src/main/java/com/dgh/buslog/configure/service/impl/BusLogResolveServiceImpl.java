package com.dgh.buslog.configure.service.impl;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.CanalEntry.Column;
import com.alibaba.otter.canal.protocol.Message;
import com.dgh.buslog.configure.service.BusLogResolveService;
import com.google.protobuf.InvalidProtocolBufferException;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.fastjson.JSON.parseObject;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @Author tiger
 * @Date 2021/2/1 14:41
 */
public class BusLogResolveServiceImpl implements BusLogResolveService {
    private Logger log = LoggerFactory.getLogger(BusLogResolveServiceImpl.class);

    private JdbcTemplate jdbcTemplate;

    private final String busLogTable;

    private final Map<String, ExecutionModel> executionMap = new HashMap<>();

    public BusLogResolveServiceImpl(JdbcTemplate jdbcTemplate, List<String> executions, String busLogTable) {
        this.jdbcTemplate = jdbcTemplate;
        this.busLogTable = busLogTable;

        //初始化 库表
        if (!existTable(busLogTable)) {
            createTable(busLogTable);
        }

        //初始化配置
        if (!isEmpty(executions)) {
            executionMap.putAll(
                executions.stream()
                    .map(it -> parseObject(it, ExecutionModel.class))
                    .collect(toMap(it -> it.getSchema() + "_" + it.getTable(), it -> it))
            );
        }
    }

    @Override
    public void resolving(Message message) {
        this.printEntity(message.getEntries());
    }

    private void printEntity(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (InvalidProtocolBufferException e) {
                log.error("canal 数据转化为RowChange出错 ", e);
                return;
            }

            String tableName = entry.getHeader().getTableName();
            String schemaName = entry.getHeader().getSchemaName();

            if(tableName.equalsIgnoreCase(busLogTable)) {
                return;
            }
            String key = schemaName + "_" + tableName;
            if (!executionMap.containsKey(key)) {
                log.debug("canal 解析数据, 配置文件中未配置数据表：{}.{} ", schemaName, tableName);
                return;
            }

            ExecutionModel model = executionMap.get(key);
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                CanalEntry.EventType eventType = rowChange.getEventType();
//                printBinlog(entry);

                switch (eventType) {
                    case INSERT:
                        String originTxt = model.getAdd();
                        if (isNotBlank(originTxt)) {
                            Map<String, String> afterMap = rowData.getAfterColumnsList().stream().collect(toMap(it -> it.getName().toUpperCase(), Column::getValue));
                            String convertTxt = new StrSubstitutor(afterMap).replace(originTxt);
                            saveBusLog(tableName, eventType.toString(), convertTxt);
                        }
                        break;
                    case UPDATE:
                        String originTxtUpdate = model.getEdit();
                        if (isNotBlank(originTxtUpdate)) {
                            Map<String, String> afterMapUpdate = rowData.getAfterColumnsList().stream().collect(toMap(it -> it.getName().toUpperCase() + "_NEW", Column::getValue));
                            Map<String, String> beforeMapUpdate = rowData.getBeforeColumnsList().stream().collect(toMap(it -> it.getName().toUpperCase() + "_OLD", Column::getValue));
                            afterMapUpdate.putAll(beforeMapUpdate);
                            String convertTxtUpdate = new StrSubstitutor(afterMapUpdate).replace(originTxtUpdate);
                            saveBusLog(tableName, eventType.toString(), convertTxtUpdate);
                        }
                        break;
                    case DELETE:
                        String originTxtDelete = model.getDelete();
                        if (isNotBlank(originTxtDelete)) {
                            Map<String, String> beforeMap = rowData.getBeforeColumnsList().stream().collect(toMap(it -> it.getName().toUpperCase(), Column::getValue));
                            String convertTxtDelete = new StrSubstitutor(beforeMap).replace(originTxtDelete);
                            saveBusLog(tableName, eventType.toString(), convertTxtDelete);
                        }
                        break;
                    default:
                        log.debug("未配置的数据操作类型 {}", eventType);
                        break;
                }

            }
        }
    }

    private void actionRowData(CanalEntry.RowData rowData, CanalEntry.EventType eventType) {
        switch (eventType) {
            //如果希望监听多种事件，可以手动增加case
            case INSERT:
                log.info("------------ {}", eventType);
                this.printColumn(rowData.getAfterColumnsList());
                break;
            case UPDATE:
                log.info("------------ {} Before ...", eventType);
                this.printColumn(rowData.getBeforeColumnsList());
                log.info("------------ {} After ...", eventType);
                this.printColumn(rowData.getAfterColumnsList());
                break;
            case DELETE:
                log.info("------------ {} ...", eventType);
                this.printColumn(rowData.getBeforeColumnsList());
                break;
            default:
                break;
        }
    }


    /**
     * 处理数据， 打印数据
     *
     * @param columns
     */
    private void printColumn(List<Column> columns) {
        for (Column column : columns) {
            log.info("index: {} , columnName: {}, value: {}, updated: {}, mysqlType: {}, isKey: {}, length: {}",
                column.getIndex(), column.getName(), column.getValue(), column.getUpdated(), column.getMysqlType(), column.getIsKey(), column.getLength());
        }
    }

    private void printBinlog(CanalEntry.Entry entry) {
        log.info(String.format("binlog[%s:%s] , name[%s,%s] , eventType : %s",
            entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
            entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
            entry.getEntryType()));
    }

    /**
     * 保存canal日志
     * @param tableName
     * @param type
     * @param content
     */
    private void saveBusLog(String tableName, String type, String content) {
        BusLogModel busLog = new BusLogModel();
        busLog.setTableName(tableName);
        busLog.setType(type);
        busLog.setContent(content);
        saveBusLog(busLog);
    }

    private void saveBusLog(BusLogModel busLog) {
        String sql = "INSERT INTO `"+ busLogTable +"`(`table_name`, `type`, `content`) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, busLog.getTableName(), busLog.getType(), busLog.getContent());
    }

    /**
     * 是否存在 数据表
     * @param tableName
     * @return
     */
    private boolean existTable(String tableName) {
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = jdbcTemplate.getDataSource().getConnection();
            rs = null;
            DatabaseMetaData data = conn.getMetaData();
            String[] types = {"TABLE"};
            rs = data.getTables(null, null, tableName, types);
            if (rs.next()) {
                return true;
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            try {
                rs.close();
                conn.close();
            } catch (SQLException e) {
                log.error("", e);
            }
        }

        return false;
    }

    /**
     * 创建一张表
     *
     * @param tableName
     */
    private int createTable(String tableName) {
        String createTable = "CREATE TABLE IF NOT EXISTS  `" + tableName + "`  (  " +
            " `id` INT ( 11 ) NOT NULL AUTO_INCREMENT,  " +
            " `table_name` VARCHAR ( 255 ) DEFAULT '' NOT NULL COMMENT '表名称',  " +
            " `type` VARCHAR ( 10 ) DEFAULT '' NOT NULL COMMENT '数据操作类型',  " +
            " `content` text COMMENT '操作内容',  " +
            " `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',  " +
            " `modify_time` TIMESTAMP NOT NULL DEFAULT '1971-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',  " +
            " PRIMARY KEY ( `id` )) ENGINE = INNODB DEFAULT CHARSET = utf8  COMMENT 'canal日志记录表'; ";
        try {
            jdbcTemplate.update(createTable);
            log.info("canal日志记录表 {} 初始化完成 ...", tableName);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    static class BusLogModel {
        private String tableName;
        private String type;
        private String content;

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    static class ExecutionModel {
        private String schema;
        private String table;
        private String add;
        private String edit;
        private String delete;

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getAdd() {
            return add;
        }

        public void setAdd(String add) {
            this.add = add;
        }

        public String getEdit() {
            return edit;
        }

        public void setEdit(String edit) {
            this.edit = edit;
        }

        public String getDelete() {
            return delete;
        }

        public void setDelete(String delete) {
            this.delete = delete;
        }
    }
}
