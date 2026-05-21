package io.github.kamioj.quickstart.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("qs_dept")
public class DeptDo {

    @TableId
    private Long id;
    private String name;
}
