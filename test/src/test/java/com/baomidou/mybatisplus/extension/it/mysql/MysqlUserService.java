package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.extension.service.IStreamService;
import com.baomidou.mybatisplus.extension.service.impl.StreamServiceImpl;
import org.springframework.stereotype.Service;

public interface MysqlUserService extends IStreamService<MysqlUserDo> {

    @Service
    class Impl extends StreamServiceImpl<MysqlUserMapper, MysqlUserDo> implements MysqlUserService {
    }
}
