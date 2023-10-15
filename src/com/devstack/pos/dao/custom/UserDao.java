package com.devstack.pos.dao.custom;

import com.devstack.pos.dao.CrudDao;
import com.devstack.pos.entity.Customer;
import com.devstack.pos.entity.User;

import java.sql.SQLException;
import java.util.List;

public interface UserDao extends CrudDao<User,String> {

}
