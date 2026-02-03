package cz.inovatika.altoEditor.db.manager;

import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.dao.VersionDao;
import cz.inovatika.altoEditor.db.filter.VersionFilter;
import cz.inovatika.altoEditor.db.model.Version;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VersionManagerTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field instanceField = VersionManager.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSetInstanceProperInitialization() {
        // Arrange
        DaoFactory daoFactoryMock = mock(DaoFactory.class);

        // Act
        VersionManager.setInstance(daoFactoryMock);
        VersionManager instance = VersionManager.getInstance();

        // Assert
        assertNotNull(instance, "VersionManager instance should not be null after proper initialization.");
    }

    @Test
    void testGetInstanceThrowsExceptionIfInstanceNotSet() {
        // Act & Assert
        assertThrows(IllegalStateException.class, VersionManager::getInstance, "set instance first!");
    }

    @Test
    void testSetInstance_withNullDaoFactory_shouldThrowNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> VersionManager.setInstance(null), "daos");
    }

    @Test
    void testSetInstanceWithNullThrowsNullPointerException() {
        // Arrange
        DaoFactory daoFactoryMock = null;

        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> VersionManager.setInstance(daoFactoryMock),
                "Expected setInstance to throw a NullPointerException when the argument is null."
        );

        assertEquals("daos", exception.getMessage(), "Exception message should indicate that 'daos' must not be null.");
    }

    @Test
    void testFindVersion_withValidFilter() {
        // Arrange
        DaoFactory daoFactoryMock = mock(DaoFactory.class);
        VersionDao versionDaoMock = mock(VersionDao.class);
        Transaction transactionMock = mock(Transaction.class);

        VersionFilter filter = VersionFilter.builder().limit(5).offset(0).build();
        List<Version> expectedVersions = List.of(new Version(), new Version());

        when(daoFactoryMock.createVersionDao()).thenReturn(versionDaoMock);
        when(daoFactoryMock.createTransaction()).thenReturn(transactionMock);
        when(versionDaoMock.findByFilter(filter)).thenReturn(expectedVersions);

        VersionManager.setInstance(daoFactoryMock);

        // Act
        List<Version> actualVersions = VersionManager.getInstance().findVersion(filter);

        // Assert
        assertNotNull(actualVersions, "Returned list should not be null.");
        assertEquals(expectedVersions, actualVersions, "findVersion should return the expected list of versions.");
        verify(versionDaoMock).setTransaction(transactionMock);
        verify(versionDaoMock).findByFilter(filter);
        verify(transactionMock).close();
    }

    @Test
    void testFindVersion_withNullFilter_throwsException() {
        // Arrange
        DaoFactory daoFactoryMock = mock(DaoFactory.class);
        VersionManager.setInstance(daoFactoryMock);

        // Act & Assert
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> VersionManager.getInstance().findVersion(null),
                "Expected findVersion to throw NullPointerException for null filter."
        );

        assertEquals("filter must not be null", exception.getMessage(), "Exception message should match the expected message.");
    }
}