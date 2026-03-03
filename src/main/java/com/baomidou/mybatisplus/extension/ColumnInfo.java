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
        if (this.tableField != null && !this.tableField.value().isEmpty()) {
            return this.tableField.value().replace(StringPool.BACKTICK, "");
        }
        return camelToUnderline(this.field.getName());
    }

    private static String camelToUnderline(String name) {
        if (name == null || name.isEmpty()) return name;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) sb.append('_');
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public Class<?> getColumnType() {
        return this.field.getType();
    }

    public String getPropertyName() {
        return this.field.getName();
    }

}
