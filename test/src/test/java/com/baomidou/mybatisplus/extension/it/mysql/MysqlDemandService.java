package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.extension.service.IStreamService;
import com.baomidou.mybatisplus.extension.service.impl.StreamServiceImpl;
import org.springframework.stereotype.Service;

public interface MysqlDemandService extends IStreamService<MysqlDemandDo> {

    @Service
    class Impl extends StreamServiceImpl<MysqlDemandMapper, MysqlDemandDo> implements MysqlDemandService {
    }
}
