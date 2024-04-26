package com.baomidou.mybatisplus.extension;

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

import java.util.*;

public class ExQueryWrapper<T> extends QueryWrapper<T> {
    private boolean addLogicDeleted = false;

    private final Map<String, List<String>> tableRenameMap = new LinkedHashMap<>();

    private String fromTable;
    private TableInfo<T> fromTableInfo;

    private final List<String> joinTables = new ArrayList<>();

    private boolean distinct;
    private boolean forUpdate;

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
        existQueryWrapper.setFromTable(this.getFromTableInfo(), this.getFromTableRename());
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
        existQueryWrapper.last("LIMIT " + existQueryWrapper.getSkip() + ", " + existQueryWrapper.getLimit());
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
        boolean containAggregate = Arrays.stream(this.getSqlSelect().split(",")).anyMatch(x -> x.trim().matches("(?!)(avg\\()|(sum\\()|(max\\()|(min\\()|(count\\().*"));
        //StringUtils.regexFind("(?i)(avg\\()|(sum\\()|(max\\()|(min\\()|(count\\()", this.getSqlSelect());
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
            countQueryWrapper.setFromTable(this.getFromTableInfo(), this.getFromTableRename());
//            countQueryWrapper.setFromTable(this.getSqlFrom());
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
            pageQueryWrapper.setFromTable(this.getFromTableInfo(), this.getFromTableRename());
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
                String columnName = keySet.stream().filter(x -> x.endsWith("/" + orderItem.getColumn() + StringPool.BACKTICK)).findFirst().orElse(null);
                pageQueryWrapper.orderBy(true, orderItem.isAsc(), columnName == null ? orderItem.getColumn() : columnName);
            }
        }
        // 设置分页大小
        pageQueryWrapper.setSkip((pageNumber - 1) * pageSize);
        pageQueryWrapper.setLimit(pageSize);
        pageQueryWrapper.last("LIMIT " + pageQueryWrapper.getSkip() + ", " + pageQueryWrapper.getLimit());

        return pageQueryWrapper;
    }

    public synchronized void addLogicDelete() {
        if (!addLogicDeleted) {
            addLogicDeleted = true;
            if (fromTableInfo != null && fromTableInfo.isWithLogicDelete() && !withDeleted) {
                String deleteColumn = (StringPool.BACKTICK + (fromTable.contains(" AS ") ? fromTable.split(" AS ")[1] : fromTable)
                        + StringPool.BACKTICK + StringPool.DOT + StringPool.BACKTICK
                        + fromTableInfo.getLogicDeleteColumn().getColumnName() + StringPool.BACKTICK)
                        .replace(StringPool.BACKTICK + StringPool.BACKTICK, StringPool.BACKTICK);
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
        String tableName = StringPool.BACKTICK + fromTableInfo.getTableName() + StringPool.BACKTICK;
        if (!StringUtils.isEmpty(rename)) {
            tableName += Constants.AS + StringPool.BACKTICK + rename + StringPool.BACKTICK;
        }
        this.fromTable = tableName.replace(StringPool.BACKTICK + StringPool.BACKTICK, StringPool.BACKTICK);
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
        return this.joinTables.size() > 0 ? this.fromTable + "\n" + String.join("\n", this.joinTables) : this.fromTable;
    }

    public String getCustomSqlFromSegment() {
        String sqlFrom = getSqlFrom();
        return StringUtils.isEmpty(sqlFrom) ? "" : "FROM\n" + sqlFrom;
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
        selectSqlList.clear();
        keySet.clear();
        limit = Long.MAX_VALUE;
        skip = 0;
        withDeleted = false;
        super.clear();
    }

}
