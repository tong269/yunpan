package com.qst.yunpan.dao;

import com.qst.yunpan.pojo.User;
import org.springframework.stereotype.Repository;
import org.apache.ibatis.annotations.Param;

@Repository
public interface UserDao {
    void addUser(User user) throws Exception;
    User checkUser(User user) throws Exception;

    User findUser(User user) throws Exception;
    String getCountSize(String username);

    void addOffice(@Param("officeId") String officeId, @Param("officeMd5") String officeMd5) throws Exception;

    String getOfficeId(String officeMd5) throws Exception;

    void reSize(@Param("username") String username, @Param("formatSize") String formatSize) throws Exception;
}