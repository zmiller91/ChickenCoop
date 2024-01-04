package coop.shared.database.repository;

import coop.shared.database.table.Pi;
import coop.shared.database.table.User;
import coop.shared.database.table.AuthorizerScopedTable;

import java.io.Serializable;

public abstract class AuthorizerScopedRepository<V extends AuthorizerScopedTable> extends GenericRepository<V> {

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

    public V findById(Pi pi, Serializable id) {
        V result = super.findById(id);
        if (result != null && result.getPi().getId().equals(pi.getId())) {
            return result;
        }

        return null;
    }

}
