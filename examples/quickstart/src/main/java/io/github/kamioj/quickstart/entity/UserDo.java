package io.github.kamioj.quickstart.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("qs_user")
public class UserDo {

    @TableId
    private Long id;
    private String name;
    private Integer age;
    private Integer salary;
    private Long deptId;

    @TableLogic
    private Integer deleted;
}
