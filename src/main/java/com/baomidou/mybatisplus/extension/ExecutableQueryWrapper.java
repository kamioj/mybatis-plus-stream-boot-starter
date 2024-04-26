package com.baomidou.mybatisplus.extension;

import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExecutableQueryWrapper<T> extends ExQueryWrapper<T> {

    private final List<String> setters = new ArrayList<>();

    private final Set<ColumnInfo> effectColumns = new HashSet<>();

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

    @Override
    public String getSqlSet() {
        return CollectionUtils.isEmpty(this.setters) ? "" : String.join(",", this.setters);
    }

    public String getSqlDuplicateSet() {
        return CollectionUtils.isEmpty(this.setters) ? "" : "ON DUPLICATE KEY UPDATE\n" + getSqlSet();
    }

    @Override
    public void clear() {
        setters.clear();
        effectColumns.clear();
        super.clear();
    }
}
