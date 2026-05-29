package com.baomidou.mybatisplus.extension.core;

import com.baomidou.mybatisplus.core.conditions.ISqlSegment;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlKeyword;
import com.baomidou.mybatisplus.core.enums.WrapperKeyword;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.toolkit.MybatisUtil;
import com.baomidou.mybatisplus.extension.dialect.DialectRegistry;
import com.baomidou.mybatisplus.extension.metadata.TableInfo;

import java.util.*;

public class ExQueryWrapper<T> extends QueryWrapper<T> {
    /** 聚合函数检测：大小写不敏感，匹配 avg/sum/max/min/count 后跟可选空白与左括号。 */
    private static final java.util.regex.Pattern AGGREGATE_FUNC_PATTERN =
        java.util.regex.Pattern.compile("(?i)(avg|sum|max|min|count)\\s*\\(");

    private boolean addLogicDeleted = false;

    private final Map<String, List<String>> tableRenameMap = new LinkedHashMap<>();

    private String fromTable;
    private TableInfo<T> fromTableInfo;

    private final List<String> joinTables = new ArrayList<>();

    private boolean distinct;
    private boolean forUpdate;
    /** 4.0：精细行锁模式。为 null 时退化到 {@code forUpdate} 布尔字段的语义（兼容 3.x）。*/
    private com.baomidou.mybatisplus.extension.dialect.LockMode lockMode;
    /** 4.0：仅在 {@code lockMode == WAIT} 时使用，单位秒 */
    private int lockWaitSeconds;

    private final List<String> selectSqlList = new ArrayList<>();
    private final Set<String> keySet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    private long limit = Long.MAX_VALUE;
    private long skip = 0;

    private boolean withDeleted = false;

    public ExQueryWrapper() {
        super();
        select("*");
    }

    public ExQueryWrapper(Class<T> tClass) {
        this();
        setFromTable(MybatisUtil.getTableInfo(tClass), null);
    }

    /**
     * 重置查询器序号
     */
    public void reset() {
        this.paramNameSeq.set(0);
    }

    /**
     * 生成存在一条查询器（查询后要调用reset方法重置查询器，否则源查询器会有异常）
     *
     * @return 存在一条查询器
     */
    public ExQueryWrapper<T> toExistWrapper() {
        this.getSqlSegment();
        this.reset();
        ExQueryWrapper<T> existQueryWrapper = new ExQueryWrapper<>();
        existQueryWrapper.withDeleted = this.withDeleted;
        existQueryWrapper.addLogicDeleted = this.addLogicDeleted;
        existQueryWrapper.select("1");
        // Phase 2: 直接复制 BACKTICK token 形态的 fromTable，避免对已加引号的别名重复加引号
        existQueryWrapper.setFromTable(this.fromTable);
        existQueryWrapper.fromTableInfo = this.fromTableInfo;
        existQueryWrapper.joinTables.addAll(this.joinTables);
        // 复制条件
        List<ISqlSegment> expression;
        if (this.getExpression().getNormal().size() > 0) {
            expression = new ArrayList<>();
            expression.add(WrapperKeyword.APPLY);
            expression.addAll(this.getExpression().getNormal());
            existQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
        }
        if (this.getExpression().getOrderBy().size() > 0) {
            for (ISqlSegment orderBy : this.getExpression().getOrderBy()) {
                expression = new ArrayList<>();
                expression.add(SqlKeyword.ORDER_BY);
                expression.add(orderBy);
                existQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
            }
        }
        if (this.getParamNameValuePairs().size() > 0) {
            existQueryWrapper.getParamNameValuePairs().putAll(this.getParamNameValuePairs());
        }
        // 删除排序
        existQueryWrapper.getExpression().getOrderBy().clear();
        // 限制只查一条记录
        existQueryWrapper.setSkip(0);
        existQueryWrapper.setLimit(1);
        existQueryWrapper.last(DialectRegistry.current().paginate("", existQueryWrapper.getSkip(), existQueryWrapper.getLimit()).trim());
        return existQueryWrapper;
    }

    /**
     * 生成数量查询器（查询后要调用reset方法重置查询器，否则源查询器会有异常）
     *
     * @param rename 数量自动重命名，默认值"C"
     * @return 数量查询器
     */
    public ExQueryWrapper<T> toCountQueryWrapper(String rename) {
        this.getSqlSegment();
        this.reset();
        boolean containAggregate = Arrays.stream(this.getSqlSelect().split(",")).anyMatch(x -> AGGREGATE_FUNC_PATTERN.matcher(x.trim()).find());
        boolean containDistinct = this.isDistinct();
        boolean containGroupBy = this.getExpression().getGroupBy().size() > 0;
        boolean containHaving = this.getExpression().getHaving().size() > 0;
        boolean containLimit = this.getLimit() != Long.MAX_VALUE || this.getSkip() != 0;

        String renameSql = StringUtils.isEmpty(rename) ? " AS C" : " AS " + rename;
        ExQueryWrapper<T> countQueryWrapper = new ExQueryWrapper<>();
        countQueryWrapper.withDeleted = this.withDeleted;
        countQueryWrapper.addLogicDeleted = this.addLogicDeleted;
        if (!containGroupBy && containAggregate) {
            // 不带分组但有聚合函数
            countQueryWrapper.select("1" + renameSql);
            countQueryWrapper.setFromTable("");
        } else if (containDistinct || containHaving) {
            // 有分组筛选或有去重
            this.addLogicDelete();
            String customSqlSegment = this.getExpression().getNormal().isEmpty() ? "" : Constants.WHERE + this.getExpression().getNormal().getSqlSegment();
            customSqlSegment += this.getExpression().getGroupBy().getSqlSegment() + this.getExpression().getHaving().getSqlSegment();
            customSqlSegment += lastSql.getStringValue();
            countQueryWrapper.select("COUNT(*)" + renameSql);
            countQueryWrapper.setFromTable("(" + "SELECT " + this.getSqlSelect() + " " + this.getCustomSqlFromSegment() + " " + customSqlSegment + ") TOTAL");
            countQueryWrapper.getParamNameValuePairs().putAll(this.getParamNameValuePairs());
        } else if (containGroupBy || containLimit) {
            // 有分组或有最大数量
            this.addLogicDelete();
            String customSqlSegment = this.getExpression().getNormal().isEmpty() ? "" : Constants.WHERE + this.getExpression().getNormal().getSqlSegment();
            customSqlSegment += this.getExpression().getGroupBy().getSqlSegment() + this.getExpression().getHaving().getSqlSegment();
            customSqlSegment += lastSql.getStringValue();
            countQueryWrapper.select("COUNT(*)" + renameSql);
            countQueryWrapper.setFromTable("(" + "SELECT 1 " + this.getCustomSqlFromSegment() + " " + customSqlSegment + ") TOTAL");
            countQueryWrapper.getParamNameValuePairs().putAll(this.getParamNameValuePairs());
        } else {
            // 无分组
            countQueryWrapper.select("COUNT(*)" + renameSql);
            countQueryWrapper.setFromTable(this.fromTable);
            countQueryWrapper.fromTableInfo = this.fromTableInfo;
            countQueryWrapper.joinTables.addAll(this.joinTables);
            // 复制条件
            List<ISqlSegment> expression;
            if (this.getExpression().getNormal().size() > 0) {
                expression = new ArrayList<>();
                expression.add(WrapperKeyword.APPLY);
                expression.addAll(this.getExpression().getNormal());
                countQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
            }
            if (this.getExpression().getOrderBy().size() > 0) {
                for (ISqlSegment orderBy : this.getExpression().getOrderBy()) {
                    expression = new ArrayList<>();
                    expression.add(SqlKeyword.ORDER_BY);
                    expression.add(orderBy);
                    countQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
                }
            }
            if (this.getParamNameValuePairs().size() > 0) {
                countQueryWrapper.getParamNameValuePairs().putAll(this.getParamNameValuePairs());
            }
            // 删除排序
            countQueryWrapper.getExpression().getOrderBy().clear();
        }
        return countQueryWrapper;
    }

    /**
     * 生成分页查询器（查询后要调用reset方法重置查询器，否则源查询器会有异常）
     *
     * @param pageNumber 页数，从1开始
     * @param pageSize   每页大小
     * @param orders     页排序
     * @return 分页查询器
     */
    public ExQueryWrapper<T> toPageQueryWrapper(Long pageNumber, Long pageSize, List<OrderItem> orders) {
        this.getSqlSegment();
        this.reset();
        boolean containLimit = this.getLimit() != Long.MAX_VALUE || this.getSkip() != 0;

        ExQueryWrapper<T> pageQueryWrapper = new ExQueryWrapper<>();
        pageQueryWrapper.withDeleted = this.withDeleted;
        pageQueryWrapper.addLogicDeleted = this.addLogicDeleted;
        if (containLimit) {
            // 有分组或有去重或有最大数量
            pageQueryWrapper.select("*");
            pageQueryWrapper.setFromTable("(" + "SELECT " + this.getSqlSelect() + " " + this.getCustomSqlFromSegment() + " " + this.getCustomSqlSegment() + ") TOTAL");
            pageQueryWrapper.getParamNameValuePairs().putAll(this.getParamNameValuePairs());
            // 设置分页大小
        } else {
            // 无分组
            pageQueryWrapper.setDistinct(this.isDistinct());
            pageQueryWrapper.selectSqlList.addAll(this.selectSqlList);
            pageQueryWrapper.updateSelect();
            pageQueryWrapper.setFromTable(this.fromTable);
            pageQueryWrapper.fromTableInfo = this.fromTableInfo;
            pageQueryWrapper.joinTables.addAll(this.joinTables);
            // 复制条件
            List<ISqlSegment> expression;
            if (this.getExpression().getNormal().size() > 0) {
                expression = new ArrayList<>();
                expression.add(WrapperKeyword.APPLY);
                expression.addAll(this.getExpression().getNormal());
                pageQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
            }
            if (this.getExpression().getOrderBy().size() > 0) {
                for (ISqlSegment orderBy : this.getExpression().getOrderBy()) {
                    expression = new ArrayList<>();
                    expression.add(SqlKeyword.ORDER_BY);
                    expression.add(orderBy);
                    pageQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
                }
            }
            if (this.getExpression().getGroupBy().size() > 0) {
                expression = new ArrayList<>(this.getExpression().getGroupBy());
                expression.add(0, SqlKeyword.GROUP_BY);
                pageQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
            }
            if (this.getExpression().getHaving().size() > 0) {
                expression = new ArrayList<>(this.getExpression().getHaving());
                expression.add(0, SqlKeyword.HAVING);
                pageQueryWrapper.getExpression().add(expression.toArray(new ISqlSegment[0]));
            }
            if (this.getParamNameValuePairs().size() > 0) {
                pageQueryWrapper.getParamNameValuePairs().putAll(this.getParamNameValuePairs());
            }
//            // 重置条件缓存
//            try {
//                ReflectUtils.setFieldValue(pageQueryWrapper.getExpression(), "cacheSqlSegment", false);
//            } catch (NoSuchFieldException | IllegalAccessException ignored) {
//            }
        }
        if (!CollectionUtils.isEmpty(orders)) {
            for (OrderItem orderItem : orders) {
                // orderItem.getColumn() 此处已被 page() 预处理成 `propName/seq` 别名形态（propName 经 buildPage 白名单校验）。
                // 精确匹配 keySet（实际投影出的别名集合，CASE_INSENSITIVE）：命中则按该别名排序；未命中（非投影列/未解析）跳过，
                // 绝不把原始串拼进 ORDER BY（防注入）。旧实现用 endsWith("/"+col+BACKTICK)，因 col 已是 `propName/seq` 而恒不命中，
                // 导致 IPage/PageVo 排序被静默丢弃（L-05）。
                if (keySet.contains(orderItem.getColumn())) {
                    pageQueryWrapper.orderBy(true, orderItem.isAsc(), orderItem.getColumn());
                }
            }
        }
        // 设置分页大小
        pageQueryWrapper.setSkip((pageNumber - 1) * pageSize);
        pageQueryWrapper.setLimit(pageSize);
        pageQueryWrapper.last(DialectRegistry.current().paginate("", pageQueryWrapper.getSkip(), pageQueryWrapper.getLimit()).trim());

        return pageQueryWrapper;
    }

    public synchronized void addLogicDelete() {
        if (!addLogicDeleted) {
            addLogicDeleted = true;
            if (fromTableInfo != null && fromTableInfo.isWithLogicDelete() && !withDeleted) {
                // Phase 2: fromTable 已是 BACKTICK token，alias 直接就是 `xxx` token 形态；
                // 拼上列名 token，由 getCustomSqlSegment() 的 translate() 统一翻译为方言引号
                String alias = fromTable.contains(" AS ") ? fromTable.split(" AS ")[1] : fromTable;
                String deleteColumn = alias + StringPool.DOT
                        + StringPool.BACKTICK + fromTableInfo.getLogicDeleteColumn().getColumnName() + StringPool.BACKTICK;
                if (expression.getNormal().size() > 0) {
//                    ArrayList<ISqlSegment> normal = new ArrayList<>(expression.getNormal());
//                    expression.getNormal().clear();
//                    and(x -> x.getExpression().add(normal.toArray(new ISqlSegment[0])));
                    expression.getNormal().add(0, () -> "(");
                    expression.getNormal().add(() -> ")");
                }
                if (fromTableInfo.getLogicNotDeleteValue().equalsIgnoreCase("null")) {
                    isNull(deleteColumn);
                } else if (fromTableInfo.getLogicNotDeleteValue().equalsIgnoreCase("not null")) {
                    isNotNull(deleteColumn);
                } else {
                    apply(deleteColumn + " = " + fromTableInfo.getLogicNotDeleteValue());
                }
            }
        }
    }

    @Override
    public synchronized String getSqlSegment() {
        addLogicDelete();
        return super.getSqlSegment();
    }

    /**
     * 获取并记录表的重命名
     *
     * @param tableName 表名
     * @param rename    表重命名
     * @return 表的重命名
     */
    public String getRename(String tableName, String rename) {
        if (tableRenameMap.containsKey(tableName) && StringUtils.isEmpty(rename)) {
            return tableRenameMap.get(tableName).get(0);
        } else {
            return rename;
        }
    }

    public void setRename(String tableName, String rename) {
        if (!tableRenameMap.containsKey(tableName)) {
            ArrayList<String> tableRenames = new ArrayList<>();
            tableRenames.add(rename);
            tableRenameMap.put(tableName, tableRenames);
        } else if (!tableRenameMap.get(tableName).contains(rename)) {
            if (StringUtils.isEmpty(rename)) {
                tableRenameMap.get(tableName).add(0, rename);
            } else {
                tableRenameMap.get(tableName).add(rename);
            }
        }
    }

    public void setFromTable(String fromTable) {
        this.fromTable = fromTable;
    }

    public void setFromTable(TableInfo<T> fromTableInfo, String rename) {
        // Phase 2: 表名 + AS 别名改存 BACKTICK token，与查询路径列名约定一致；
        // 实际方言引号在 getSqlFrom() 经 DialectQuoteTranslator.translate() 统一翻译。
        // rename 必须是裸别名（不带引号）—— 所有调用方都传裸名。
        String tableName = StringPool.BACKTICK + fromTableInfo.getTableName() + StringPool.BACKTICK;
        if (!StringUtils.isEmpty(rename)) {
            tableName += Constants.AS + StringPool.BACKTICK + rename + StringPool.BACKTICK;
        }
        this.fromTable = tableName;
        this.fromTableInfo = fromTableInfo;
    }

    public TableInfo<T> getFromTableInfo() {
        return fromTableInfo;
    }

    public String getFromTableRename() {
        String[] s = fromTable.split(" ");
        return s[s.length - 1];
    }

    public void addJoinSql(String joinSql) {
        joinTables.add(joinSql);
    }

    public String getSqlFrom() {
        // 4.1: 通过 DialectQuoteTranslator 适配器把 wrapper 内部的 `name` token 翻译为方言引号
        // MySQL 方言下是 no-op；PG/DM 下转为 "name"
        String raw = this.joinTables.size() > 0 ? this.fromTable + "\n" + String.join("\n", this.joinTables) : this.fromTable;
        return com.baomidou.mybatisplus.extension.dialect.DialectQuoteTranslator.translate(raw);
    }

    public String getCustomSqlFromSegment() {
        String sqlFrom = getSqlFrom();
        return StringUtils.isEmpty(sqlFrom) ? "" : "FROM\n" + sqlFrom;
    }

    /**
     * 4.1: override 父类 getSqlSelect 走 dialect quote translator。
     * 父类返回的 SELECT 子句包含 BACKTICK 包装的列名，本方法翻译为当前方言引号。
     */
    @Override
    public String getSqlSelect() {
        return com.baomidou.mybatisplus.extension.dialect.DialectQuoteTranslator.translate(super.getSqlSelect());
    }

    /**
     * 4.1: override 父类 getCustomSqlSegment 走 dialect quote translator。
     * 父类返回的 WHERE/GROUP BY/HAVING/ORDER BY 等子句中的列名翻译为方言引号。
     */
    @Override
    public String getCustomSqlSegment() {
        return com.baomidou.mybatisplus.extension.dialect.DialectQuoteTranslator.translate(super.getCustomSqlSegment());
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getSkip() {
        return skip;
    }

    public void setSkip(long skip) {
        this.skip = skip;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
        this.updateSelect();
    }

    public com.baomidou.mybatisplus.extension.dialect.LockMode getLockMode() {
        return lockMode;
    }

    public void setLockMode(com.baomidou.mybatisplus.extension.dialect.LockMode lockMode) {
        this.lockMode = lockMode;
        this.forUpdate = (lockMode != null);
    }

    public int getLockWaitSeconds() {
        return lockWaitSeconds;
    }

    public void setLockWaitSeconds(int lockWaitSeconds) {
        this.lockWaitSeconds = lockWaitSeconds;
    }

    public boolean isForUpdate() {
        return forUpdate;
    }

    public void setForUpdate(boolean forUpdate) {
        this.forUpdate = forUpdate;
    }

    /**
     * 插入查询
     *
     * @param select 查询
     * @param key    关键字
     */
    public void addSelect(String select, String key) {
        if (!keySet.contains(key)) {
            keySet.add(key);
            selectSqlList.add(select + " AS " + key);
            updateSelect();
        }
    }

    /**
     * 插入查询
     */
    public void updateSelect() {
        if (selectSqlList.size() > 0) {
            String[] selectArray = selectSqlList.toArray(new String[0]);
            if (distinct) {
                selectArray[0] = "DISTINCT " + selectArray[0];
            }
            select(selectArray);
        }
    }

    /**
     * L-02: 判断是否真正设置了 GROUP BY 子句。
     * 用于区分「group 回调只设了 HAVING 而未设 groupBy」与「完全无分组」两种情形，
     * 避免误判 distinct 逻辑。
     *
     * @return 是否有 GROUP BY
     */
    public boolean hasGroupBy() {
        return getExpression().getGroupBy().size() > 0;
    }

    public boolean isWithDeleted() {
        return withDeleted;
    }

    public void setWithDeleted(boolean withDeleted) {
        this.withDeleted = withDeleted;
    }

    public String getLastSql() {
        return lastSql.getStringValue();
    }

    @Override
    public void clear() {
        addLogicDeleted = false;
        tableRenameMap.clear();
        fromTable = null;
        fromTableInfo = null;
        joinTables.clear();
        distinct = false;
        forUpdate = false;
        lockMode = null;
        lockWaitSeconds = 0;
        selectSqlList.clear();
        keySet.clear();
        limit = Long.MAX_VALUE;
        skip = 0;
        withDeleted = false;
        super.clear();
    }

}
