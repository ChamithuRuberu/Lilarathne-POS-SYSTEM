package com.devstack.pos.dao.custom.impl;
import com.devstack.pos.dao.CrudUtil;
import com.devstack.pos.dao.custom.CustomerDao;
import com.devstack.pos.entity.Customer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerDaoImpl implements CustomerDao {



    @Override
    public boolean delete(String email) throws SQLException, ClassNotFoundException {
        return CrudUtil.execute("DELETE FROM Customer WHERE email=?",email);
    }

    @Override
    public Customer find(String email) throws SQLException, ClassNotFoundException {
        ResultSet resultSet = CrudUtil.execute("SELECT * FROM customer WHERE email=?",email);
        if (resultSet.next()) {
            return new Customer(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getDouble(4)
            );
        }
        return null;
    }

    @Override
    public List<Customer> findAll() throws SQLException, ClassNotFoundException {
        ResultSet resultSet = CrudUtil.execute("SELECT * FROM customer");

        List<Customer> customerList = new ArrayList<>();
        while (resultSet.next()) {
            customerList.add(new Customer(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getDouble(4)
            ));
        }
        return customerList;
    }

    @Override
    public List<Customer> searchCustomers(String searchText) throws SQLException, ClassNotFoundException {
        searchText = "%" + searchText + "%";
        ResultSet resultSet = CrudUtil.execute("SELECT * FROM customer WHERE email LIKE ? || name LIKE ?",searchText,searchText);

        List<Customer> dtos = new ArrayList<>();
        while (resultSet.next()) {
            dtos.add(new Customer(
                    resultSet.getString(1),
                    resultSet.getString(2),
                    resultSet.getString(3),
                    resultSet.getDouble(4)
            ));
        }
        return dtos;
    }

    @Override
    public boolean save(Customer customer) throws SQLException, ClassNotFoundException {
        return CrudUtil.execute("INSERT INTO customer VALUES (?,?,?,?)",
                customer.getEmail(),customer.getName(),customer.getContact(),customer.getSalary());
    }

    @Override
    public boolean update(Customer customer) throws SQLException, ClassNotFoundException {
        return CrudUtil.execute("UPDATE customer SET name=?, contact=?, salary=? WHERE email=?",
                customer.getName(),customer.getContact(),customer.getSalary(),customer.getEmail());
    }


}
