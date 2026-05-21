package io.github.kamioj.quickstart.service;

import com.baomidou.mybatisplus.extension.service.impl.MysqlServiceBaseImpl;
import io.github.kamioj.quickstart.entity.UserDo;
import io.github.kamioj.quickstart.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService extends MysqlServiceBaseImpl<UserMapper, UserDo> {
}
