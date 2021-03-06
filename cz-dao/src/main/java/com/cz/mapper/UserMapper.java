package com.cz.mapper;

import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;
import com.cz.model.personal.User;

import java.util.List;

/**
 * Created by jomalone_jia on 2017/6/19.
 */
public interface UserMapper extends BaseMapper<User>{
    User loadUserByUsername(String username);
    List<User> listAllUser();
    List<User> listAllUser(Pagination page);
}
