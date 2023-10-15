package com.devstack.pos.dao;
import com.devstack.pos.db.DbConnection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CrudUtil {
    public static <T> T execute(String sql, Object...params) throws SQLException, ClassNotFoundException {
        PreparedStatement preparedStatement =
                DbConnection.getInstance().getConnection().prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            preparedStatement.setObject((i+1),params[i]);
        }
        if (sql.startsWith("SELECT")){
            return (T) preparedStatement.executeQuery();
        }
        return (T)(Boolean)(preparedStatement.executeUpdate()>0);
    }
}
