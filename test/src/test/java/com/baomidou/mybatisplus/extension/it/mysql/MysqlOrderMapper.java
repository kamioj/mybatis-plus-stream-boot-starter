package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.extension.mapper.StreamBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MysqlOrderMapper extends StreamBaseMapper<MysqlOrderDo> {
}
