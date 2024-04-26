package com.baomidou.mybatisplus.extension.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.ExQueryWrapper;
import com.baomidou.mybatisplus.extension.ExecutableQueryWrapper;
import com.baomidou.mybatisplus.extension.ProcedureParamDef;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;
import java.util.Map;

public interface MysqlBaseMapper<T> extends BaseMapper<T> {

    @Select("SELECT\n" +
            " ${ew.sqlSelect}\n" +
            "${ew.customSqlFromSegment}\n" +
            "${ew.customSqlSegment}")
    List<Map<String, Object>> list(@Param(Constants.WRAPPER) ExQueryWrapper<?> queryWrapper);

    @Select("SELECT\n" +
            " ${ew.sqlSelect}\n" +
            "${ew.customSqlFromSegment}\n" +
            "${ew.customSqlSegment}")
    IPage<Map<String, Object>> page(IPage<Map<String, Object>> page, @Param(Constants.WRAPPER) ExQueryWrapper<?> queryWrapper);

    @Insert("<script>" +
            "INSERT INTO ${ew.sqlFrom}\n" +
            "<foreach collection='columns' item='column' index='' open='(' close=')' separator=','>${column}</foreach>\n" +
            "VALUES" +
            "<foreach collection='values' item='item' index='' separator=','>" +
            "\n(<foreach collection='item' item='value' index='' separator=','>#{value}</foreach>)" +
            "</foreach>\n" +
            "${ew.sqlDuplicateSet}" +
            "</script>")
    int insertDuplicate(String[] columns, Object[][] values, @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

    @Insert("<script>" +
            "INSERT IGNORE INTO ${ew.sqlFrom}\n" +
            "<foreach collection='columns' item='column' index='' open='(' close=')' separator=','>${column}</foreach>\n" +
            "VALUES" +
            "<foreach collection='values' item='item' index='' separator=','>" +
            "\n(<foreach collection='item' item='value' index='' separator=','>#{value}</foreach>)" +
            "</foreach>" +
            "</script>")
    int insertIgnore(String[] columns, Object[][] values, @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

    @Insert("<script>" +
            "REPLACE INTO ${ew.sqlFrom}\n" +
            "<foreach collection='columns' item='column' index='' open='(' close=')' separator=','>${column}</foreach>\n" +
            "VALUES" +
            "<foreach collection='values' item='item' index='' separator=','>" +
            "\n(<foreach collection='item' item='value' index='' separator=','>#{value}</foreach>)" +
            "</foreach>" +
            "</script>")
    int insertReplace(String[] columns, Object[][] values, @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

    @Update("<script>" +
            "UPDATE ${ew.sqlFrom}\n" +
            "SET\n" +
            "${ew.sqlSet}\n" +
            "${ew.customSqlSegment}" +
            "</script>")
    int updateBatch(@Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

    @Select("<script>" +
            "{CALL ${procedureName}" +
            "<foreach collection='definitions' index='' item='def' open='(' close=')' separator=','>" +
            "#{params[${def.key}],mode=${def.mode},jdbcType=${def.jdbcType}}" +
            "</foreach>" +
            "}" +
            "</script>")
    @Options(statementType = StatementType.CALLABLE)
    List<Map<String, Object>> callProcedureForList(String procedureName, List<ProcedureParamDef> definitions, Map<String, Object> params);

}
