package com.baomidou.mybatisplus.extension;

import org.apache.ibatis.type.JdbcType;

public class ProcedureParamDef {
    private String key;
    private MODE mode;
    private JdbcType jdbcType;

    public ProcedureParamDef(String key, MODE mode, JdbcType jdbcType) {
        this.key = key;
        this.mode = mode;
        this.jdbcType = jdbcType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public MODE getMode() {
        return mode;
    }

    public void setMode(MODE mode) {
        this.mode = mode;
    }

    public JdbcType getJdbcType() {
        return jdbcType;
    }

    public void setJdbcType(JdbcType jdbcType) {
        this.jdbcType = jdbcType;
    }

    public enum MODE {
        IN, OUT, INOUT
    }
}
