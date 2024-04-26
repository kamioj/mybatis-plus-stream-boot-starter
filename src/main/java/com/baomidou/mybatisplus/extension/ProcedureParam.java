package com.baomidou.mybatisplus.extension;

import org.apache.ibatis.type.JdbcType;

public class ProcedureParam extends ProcedureParamDef {
    private Object value;

    public ProcedureParam(String key, Object value, MODE mode, JdbcType jdbcType) {
        super(key, mode, jdbcType);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
