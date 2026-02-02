package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.filter.UserFilter;
import cz.inovatika.altoEditor.db.model.User;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

public interface UserDao extends Dao {

    User createUser();

    void update(User user) throws ConcurrentModificationException;

    Optional<User> findById(Integer userId);

    List<User> findByFilter(UserFilter filter);

    int countByFilter(UserFilter filter);

    void deleteById(Integer batchId);
}
