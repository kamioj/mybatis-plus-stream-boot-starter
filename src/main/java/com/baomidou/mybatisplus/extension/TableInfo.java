package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.annotation.TableName;

import java.util.List;

public class TableInfo<T> {
    private Class<T> entityClass;
    private TableName tableName;
    private List<ColumnInfo> columns;
    private ColumnInfo keyColumn;
    private ColumnInfo logicDeleteColumn;
    private String logicDeleteValue;
    private String logicNotDeleteValue;
    private boolean withLogicDelete;

    public Class<T> getEntityClass() {
        return entityClass;
    }

    public void setEntityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public void setTableName(TableName tableName) {
        this.tableName = tableName;
    }

    public List<ColumnInfo> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnInfo> columns) {
        this.columns = columns;
    }

    public ColumnInfo getKeyColumn() {
        return keyColumn;
    }

    public void setKeyColumn(ColumnInfo keyColumn) {
        this.keyColumn = keyColumn;
    }

    public ColumnInfo getLogicDeleteColumn() {
        return logicDeleteColumn;
    }

    public void setLogicDeleteColumn(ColumnInfo logicDeleteColumn) {
        this.logicDeleteColumn = logicDeleteColumn;
    }

    public String getLogicDeleteValue() {
        return logicDeleteValue;
    }

    public void setLogicDeleteValue(String logicDeleteValue) {
        this.logicDeleteValue = logicDeleteValue;
    }

    public String getLogicNotDeleteValue() {
        return logicNotDeleteValue;
    }

    public void setLogicNotDeleteValue(String logicNotDeleteValue) {
        this.logicNotDeleteValue = logicNotDeleteValue;
    }

    public boolean isWithLogicDelete() {
        return withLogicDelete;
    }

    public void setWithLogicDelete(boolean withLogicDelete) {
        this.withLogicDelete = withLogicDelete;
    }

    public String getTableName() {
        return this.tableName != null && !this.tableName.value().isEmpty() ? this.tableName.value() : this.entityClass.getSimpleName();
    }
}
