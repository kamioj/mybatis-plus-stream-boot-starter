package com.baomidou.mybatisplus.extension.it;

import com.baomidou.mybatisplus.extension.service.IStreamService;
import com.baomidou.mybatisplus.extension.service.impl.StreamServiceImpl;
import org.springframework.stereotype.Service;

public interface UserService extends IStreamService<UserDo> {

    @Service
    class Impl extends StreamServiceImpl<UserMapper, UserDo> implements UserService {
    }
}
