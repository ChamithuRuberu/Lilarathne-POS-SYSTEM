package com.devstack.pos.dao.custom;

import com.devstack.pos.dao.CrudDao;
import com.devstack.pos.dto.CustomerDto;
import com.devstack.pos.entity.Customer;

import java.sql.SQLException;
import java.util.List;

public interface CustomerDao extends CrudDao<Customer, String> {
    //-------------------------
    public List<Customer> searchCustomers(String searchText) throws SQLException, ClassNotFoundException;

}
