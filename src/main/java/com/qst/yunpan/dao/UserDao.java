package com.qst.yunpan.dao;

import com.qst.yunpan.pojo.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserDao {
    void addUser(User user) throws Exception;
    User checkUser(User user) throws Exception;

    User findUser(User user) throws Exception;
}