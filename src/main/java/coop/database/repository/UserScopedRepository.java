package coop.database.repository;

import coop.database.table.User;
import coop.database.table.UserScopedTable;

import java.io.Serializable;

public abstract class UserScopedRepository<V extends UserScopedTable> extends GenericRepository<V> {

    @Override
    public V findById(Serializable id) {
        throw new RuntimeException("findById(id) is not callable by repositories that are scoped by a user.");
    }

    public V findById(User user, Serializable id) {
        V result = super.findById(id);
        if (result != null && result.getUser().getId().equals(user.getId())) {
            return result;
        }

        return null;
    }

}
