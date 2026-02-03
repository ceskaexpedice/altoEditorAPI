package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.filter.DigitalObjectFilter;
import cz.inovatika.altoEditor.db.model.DigitalObject;
import cz.inovatika.altoEditor.models.DigitalObjectView;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

/**
 * Interface defining the Data Access Object (DAO) operations for managing
 * instances of {@link DigitalObject} in a persistent data store.
 *
 * This interface extends the {@link Dao} contract for participating in database
 * transaction contexts and facilitates operations such as object creation,
 * retrieval, updating, and deletion, that are specific to digital objects.
 */
public interface DigitalObjectDao extends Dao {

    DigitalObject createDigitalObject();

    void update(DigitalObject digitalObject) throws ConcurrentModificationException;

    Optional<DigitalObject> findById(Integer digitalObjectId);

    List<DigitalObjectView> findByFilter(DigitalObjectFilter filter);

    int countByFilter(DigitalObjectFilter filter);

    void deleteById(Integer digitalObjectId);
}
