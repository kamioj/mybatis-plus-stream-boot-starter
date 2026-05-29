package com.baomidou.mybatisplus.extension.it.mysql;

import com.baomidou.mybatisplus.extension.service.IStreamService;
import com.baomidou.mybatisplus.extension.service.impl.StreamServiceImpl;
import org.springframework.stereotype.Service;

public interface MysqlOrderService extends IStreamService<MysqlOrderDo> {

    @Service
    class Impl extends StreamServiceImpl<MysqlOrderMapper, MysqlOrderDo> implements MysqlOrderService {
    }
}
