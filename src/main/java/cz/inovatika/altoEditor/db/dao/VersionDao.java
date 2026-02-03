package cz.inovatika.altoEditor.db.dao;

import cz.inovatika.altoEditor.db.filter.VersionFilter;
import cz.inovatika.altoEditor.db.model.Version;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface for managing {@link Version} entities in a storage system.
 * Provides methods for creating, retrieving, updating, and deleting version records,
 * as well as for querying versions using filters.
 */
public interface VersionDao extends Dao {

    Version createVersion();

    void update(Version version) throws ConcurrentModificationException;

    Optional<Version> findById(Integer versionId);

    List<Version> findByFilter(VersionFilter filter);

    int countByFilter(VersionFilter filter);

    void deleteById(Integer versionId);
}
