package com.devstack.pos.bo.custom.impl;

import com.devstack.pos.bo.custom.UserBo;
import com.devstack.pos.dao.DaoFactory;
import com.devstack.pos.dao.custom.UserDao;
import com.devstack.pos.dto.UserDto;
import com.devstack.pos.entity.User;
import com.devstack.pos.enums.DaoType;

import java.sql.SQLException;
import java.util.List;

public class UserBoImpl implements UserBo {

    UserDao userDao=  DaoFactory.getInstance().getDao(DaoType.USER);

    @Override
    public boolean saveUser(UserDto dto) throws SQLException, ClassNotFoundException {
        return userDao.save(new User(dto.getEmail(), dto.getPassword()));
    }

    @Override
    public boolean updateUser(UserDto dto) {
        return false;
    }

    @Override
    public boolean deleteUser(String email) {
        return false;
    }

    @Override
    public UserDto findUser(String email) throws SQLException, ClassNotFoundException {
        User user = userDao.find(email);
        if (user!=null) {
            return new UserDto(
                    user.getEmail(),
                    user.getPassword()
            );
        }
        return null;
    }

    @Override
    public List<UserDto> findAllUsers() {
        return null;
    }
}
