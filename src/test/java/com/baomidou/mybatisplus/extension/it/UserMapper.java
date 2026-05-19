package com.baomidou.mybatisplus.extension.it;

import com.baomidou.mybatisplus.extension.mapper.StreamBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends StreamBaseMapper<UserDo> {
}
