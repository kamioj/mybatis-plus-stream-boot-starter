package com.baomidou.mybatisplus.extension.core;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.baomidou.mybatisplus.extension.dialect.DialectQuoteTranslator;
import com.baomidou.mybatisplus.extension.dialect.DialectRegistry;
import com.baomidou.mybatisplus.extension.dialect.SqlDialect;
import com.baomidou.mybatisplus.extension.dialect.WriteMode;
import com.baomidou.mybatisplus.extension.metadata.ColumnInfo;

public class ExecutableQueryWrapper<T> extends ExQueryWrapper<T> {

    private final List<String> setters = new ArrayList<>();

    private final Set<ColumnInfo> effectColumns = new HashSet<>();

    /**
     * 4.0.2 起：批量写入操作的语义模式（DUPLICATE / IGNORE / REPLACE）。
     * 由 {@code MybatisExecutableStream.executeInsert/Ignore/Replace} 在调 mapper 前设置；
     * mapper 的 SQL 模板通过 {@link #getSqlInsertPrefix()} / {@link #getSqlConflictClause()}
     * 间接调当前 {@link SqlDialect} 渲染对应方言的 SQL 片段。
     */
    private WriteMode writeMode = WriteMode.INSERT;

    public ExecutableQueryWrapper() {
    }

    public ExecutableQueryWrapper(Class<T> tClass) {
        super(tClass);
    }

    public void addSetter(String setter) {
        this.setters.add(setter);
    }

    public List<String> getSetters() {
        return setters;
    }

    public void addEffectColumn(ColumnInfo columnInfo) {
        effectColumns.add(columnInfo);
    }

    public Set<ColumnInfo> getEffectColumns() {
        return effectColumns;
    }

    public WriteMode getWriteMode() {
        return writeMode;
    }

    public void setWriteMode(WriteMode writeMode) {
        this.writeMode = writeMode == null ? WriteMode.INSERT : writeMode;
    }

    @Override
    public String getSqlSet() {
        if (CollectionUtils.isEmpty(this.setters)) return "";
        // 4.1: setters 中的列名是 BACKTICK 风格 token，通过 translator 适配到方言引号
        return com.baomidou.mybatisplus.extension.dialect.DialectQuoteTranslator.translate(String.join(",", this.setters));
    }

    /**
     * 兼容保留（3.x 至 4.0.1）。<b>4.0.2 起内部调 {@link #getSqlConflictClause()} 走 dialect</b>。
     * @deprecated 4.0.2 起 mapper SQL 模板已改用 {@code ${ew.sqlConflictClause}}；本方法仅作为
     *     向后兼容残留，3.x → 4.x 升级期间外部代码引用可继续工作。4.1 起将移除。
     */
    @Deprecated(since = "4.0.2", forRemoval = true)
    public String getSqlDuplicateSet() {
        return getSqlConflictClause();
    }

    /** 4.0.2 新增：mapper 模板用 {@code ${ew.sqlInsertPrefix}} 获取动词前缀（"INSERT INTO" / "INSERT IGNORE INTO" / "REPLACE INTO"）*/
    public String getSqlInsertPrefix() {
        return DialectRegistry.current().insertPrefix(writeMode);
    }

    /**
     * 4.0.2 新增：mapper 模板用 {@code ${ew.sqlConflictClause}} 获取冲突处理子句。
     * <ul>
     *   <li>MySQL DUPLICATE: {@code "ON DUPLICATE KEY UPDATE ..."}；IGNORE/REPLACE 返回空（语义在前缀表达）</li>
     *   <li>PG DUPLICATE: {@code "ON CONFLICT (pk) DO UPDATE SET ..."}；IGNORE: {@code "ON CONFLICT DO NOTHING"}；REPLACE: 全列覆盖</li>
     *   <li>DM 三种 mode 都抛 {@link UnsupportedOperationException}（4.0.3 起 MERGE INTO 完整实现）</li>
     * </ul>
     */
    public String getSqlConflictClause() {
        SqlDialect d = DialectRegistry.current();
        ColumnInfo pk = (getFromTableInfo() == null) ? null : getFromTableInfo().getKeyColumn();
        String[] allColumns = (getFromTableInfo() == null) ? new String[0] :
            getFromTableInfo().getColumns().stream()
                .map(ColumnInfo::getColumnName)
                .toArray(String[]::new);
        // 4.1: setters 中可能含 BACKTICK token；translator 翻译为方言引号
        return DialectQuoteTranslator.translate(d.conflictClause(writeMode, setters, pk, allColumns));
    }

    @Override
    public void clear() {
        setters.clear();
        effectColumns.clear();
        writeMode = WriteMode.INSERT;
        super.clear();
    }
}
