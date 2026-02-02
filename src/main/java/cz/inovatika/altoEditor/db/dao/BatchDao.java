package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.filter.BatchFilter;
import cz.inovatika.altoEditor.db.model.Batch;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface for managing {@link Batch} entities in a storage system.
 * Provides methods for creating, retrieving, updating, and deleting batch records.
 */
public interface BatchDao extends Dao {

    Batch createBatch();

    void update(Batch batch) throws ConcurrentModificationException;

    Optional<Batch> findById(Integer batchId);

    List<Batch> findByFilter(BatchFilter filter);

    int countByFilter(BatchFilter filter);

    void deleteById(Integer batchId);
}
