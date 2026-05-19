package com.baomidou.mybatisplus.extension.it;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 集成测试用 entity：覆盖 PK / 普通列 / 数值列 / 布尔列。
 */
@TableName("ms_user")
public class UserDo {

    @TableId(type = IdType.INPUT)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("age")
    private Integer age;

    @TableField("active")
    private Boolean active;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public static UserDo of(long id, String name, int age, boolean active) {
        UserDo u = new UserDo();
        u.id = id;
        u.name = name;
        u.age = age;
        u.active = active;
        return u;
    }
}
