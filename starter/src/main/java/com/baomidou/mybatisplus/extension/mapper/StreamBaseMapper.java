package com.baomidou.mybatisplus.extension.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.dialect.MergeIntoSqlProvider;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.core.ExQueryWrapper;
import com.baomidou.mybatisplus.extension.core.ExecutableQueryWrapper;
import com.baomidou.mybatisplus.extension.metadata.ProcedureParamDef;

public interface StreamBaseMapper<T> extends BaseMapper<T> {

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

    /**
     * 4.0.2 起：三种批量写入（saveDuplicate / saveIgnore / saveReplace）共用同一 SQL 模板，
     * 由当前 {@code SqlDialect} 通过 {@code ${ew.sqlInsertPrefix}}（动词前缀）与
     * {@code ${ew.sqlConflictClause}}（末尾冲突子句）决定方言行为。
     * 三个方法名仍保留作为调用入口（向后兼容 + 语义清晰）。
     */
    @Insert("<script>" +
            "${ew.sqlInsertPrefix} ${ew.sqlFrom}\n" +
            "<foreach collection='columns' item='column' index='' open='(' close=')' separator=','>${column}</foreach>\n" +
            "VALUES" +
            "<foreach collection='values' item='item' index='' separator=','>" +
            "\n(<foreach collection='item' item='value' index='' separator=','>#{value}</foreach>)" +
            "</foreach>" +
            "${ew.sqlConflictClause}" +
            "</script>")
    int insertDuplicate(@Param("columns") String[] columns, @Param("values") Object[][] values, @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

    @Insert("<script>" +
            "${ew.sqlInsertPrefix} ${ew.sqlFrom}\n" +
            "<foreach collection='columns' item='column' index='' open='(' close=')' separator=','>${column}</foreach>\n" +
            "VALUES" +
            "<foreach collection='values' item='item' index='' separator=','>" +
            "\n(<foreach collection='item' item='value' index='' separator=','>#{value}</foreach>)" +
            "</foreach>" +
            "${ew.sqlConflictClause}" +
            "</script>")
    int insertIgnore(@Param("columns") String[] columns, @Param("values") Object[][] values, @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

    @Insert("<script>" +
            "${ew.sqlInsertPrefix} ${ew.sqlFrom}\n" +
            "<foreach collection='columns' item='column' index='' open='(' close=')' separator=','>${column}</foreach>\n" +
            "VALUES" +
            "<foreach collection='values' item='item' index='' separator=','>" +
            "\n(<foreach collection='item' item='value' index='' separator=','>#{value}</foreach>)" +
            "</foreach>" +
            "${ew.sqlConflictClause}" +
            "</script>")
    int insertReplace(@Param("columns") String[] columns, @Param("values") Object[][] values, @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

    /**
     * 4.0.3 起：DM 等不能用"INSERT + 末尾子句"模板的方言走这条 MERGE INTO 路径。
     * 由当前 {@code SqlDialect.buildMergeIntoScript} 通过 {@code @InsertProvider} 生成完整 SQL。
     * {@link com.baomidou.mybatisplus.extension.stream.MybatisExecutableStream} 根据
     * {@code SqlDialect.useMergeInto(WriteMode)} 决定是否走本方法。
     */
    @InsertProvider(type = MergeIntoSqlProvider.class, method = "buildSql")
    int mergeInto(@Param("columns") String[] columns, @Param("values") Object[][] values, @Param(Constants.WRAPPER) ExecutableQueryWrapper<?> queryWrapper);

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
