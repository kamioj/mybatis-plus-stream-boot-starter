package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.toolkit.StringPool;

import java.lang.reflect.Field;

public class ColumnInfo {
    private boolean key;
    private boolean logicDelete;
    private Field field;
    private TableField tableField;

    public boolean isKey() {
        return key;
    }

    public void setKey(boolean key) {
        this.key = key;
    }

    public boolean isLogicDelete() {
        return logicDelete;
    }

    public void setLogicDelete(boolean logicDelete) {
        this.logicDelete = logicDelete;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
    }

    public TableField getTableField() {
        return tableField;
    }

    public void setTableField(TableField tableField) {
        this.tableField = tableField;
    }

    public FieldFill getFieldFill() {
        return this.tableField != null ? this.tableField.fill() : FieldFill.DEFAULT;
    }

    public String getColumnName() {
        return this.tableField != null && !this.tableField.value().isEmpty() ? this.tableField.value().replace(StringPool.BACKTICK, "") : this.field.getName();
    }

    public Class<?> getColumnType() {
        return this.field.getType();
    }

    public String getPropertyName() {
        return this.field.getName();
    }

}
