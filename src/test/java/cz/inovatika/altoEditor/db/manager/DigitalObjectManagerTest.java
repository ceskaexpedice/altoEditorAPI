package cz.inovatika.altoEditor.db.manager;

import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.DigitalObjectDao;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.model.DigitalObject;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the {@code DigitalObjectManager} class.
 *
 * This class contains test cases to validate the behavior and functionality of
 * the {@code DigitalObjectManager} class, including its interactions with
 * DAO factories, transactions, and digital object data access objects.
 * Test cases cover scenarios for proper instance handling, exception handling,
 * and expected behaviors of its methods.
 */
class DigitalObjectManagerTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field instanceField = DigitalObjectManager.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSetInstance_shouldThrowNullPointerException_whenDaoFactoryIsNull() {
        // Null DaoFactory passed to setInstance should throw NullPointerException
        assertThrows(NullPointerException.class, () -> DigitalObjectManager.setInstance(null));
    }

    @Test
    void testSetInstance_shouldSuccessfullySetInstance_whenDaoFactoryIsValid() {
        // Mock a valid DaoFactory
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);

        // Assert that no exception occurs when setting a valid instance
        assertDoesNotThrow(() -> DigitalObjectManager.setInstance(mockDaoFactory));
    }

    @Test
    void testGetInstance_shouldThrowIllegalStateException_whenInstanceNotSet() {

        // Assert that accessing getInstance before setting with setInstance throws an exception
        assertThrows(IllegalStateException.class, DigitalObjectManager::getInstance);
    }

    @Test
    void testGetInstance_shouldReturnValidInstance_whenSetInstanceIsCalledBefore() {
        // Mock a valid DaoFactory
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);

        // Set the instance
        DigitalObjectManager.setInstance(mockDaoFactory);

        // Assert that the instance is successfully retrieved
        assertDoesNotThrow(DigitalObjectManager::getInstance);
    }
    @Test
    void testGetDigitalObject_shouldRetrieveDigitalObject_whenIdIsValid() {
        // Mock the required components
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);
        DigitalObjectDao mockDigitalObjectDao = Mockito.mock(DigitalObjectDao.class);
        Transaction mockTransaction = Mockito.mock(Transaction.class);
        DigitalObject mockDigitalObject = Mockito.mock(DigitalObject.class);

        // Stub the DAO factory to return the mock DAO and transaction
        Mockito.when(mockDaoFactory.createDigitalObjectDao()).thenReturn(mockDigitalObjectDao);
        Mockito.when(mockDaoFactory.createTransaction()).thenReturn(mockTransaction);

        // Stub the DAO behavior for a valid ID
        Mockito.when(mockDigitalObject.getId()).thenReturn(1);
        Mockito.when(mockDigitalObjectDao.findById(1)).thenReturn(java.util.Optional.of(mockDigitalObject));

        // Set the mocked factory to the instance
        DigitalObjectManager.setInstance(mockDaoFactory);

        // Call the method and assert no exception is thrown
        DigitalObjectManager manager = DigitalObjectManager.getInstance();
        DigitalObject result = manager.getDigitalObject(1);

        // Validate the result
        Mockito.verify(mockDigitalObjectDao).findById(1);
        Mockito.verify(mockTransaction).close();
        assert result.getId().equals(1);
    }

    @Test
    void testGetDigitalObject_shouldThrowException_whenIdDoesNotExist() {
        // Mock the required components
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);
        DigitalObjectDao mockDigitalObjectDao = Mockito.mock(DigitalObjectDao.class);
        Transaction mockTransaction = Mockito.mock(Transaction.class);

        // Stub the DAO factory to return the mock DAO and transaction
        Mockito.when(mockDaoFactory.createDigitalObjectDao()).thenReturn(mockDigitalObjectDao);
        Mockito.when(mockDaoFactory.createTransaction()).thenReturn(mockTransaction);

        // Stub the DAO behavior for a non-existent ID
        Mockito.when(mockDigitalObjectDao.findById(999)).thenReturn(java.util.Optional.empty());

        // Set the mocked factory to the instance
        DigitalObjectManager.setInstance(mockDaoFactory);

        // Call the method and assert that an exception is thrown
        DigitalObjectManager manager = DigitalObjectManager.getInstance();
        assertThrows(IllegalStateException.class, () -> manager.getDigitalObject(999));

        // Verify interactions
        Mockito.verify(mockDigitalObjectDao).findById(999);
        Mockito.verify(mockTransaction).close();
    }

    @Test
    void testGetDigitalObject_shouldThrowNullPointerException_whenIdIsNull() {
        // Mock the required components
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);

        // Set the mocked factory to the instance
        DigitalObjectManager.setInstance(mockDaoFactory);

        // Call the method and assert that a NullPointerException is thrown
        DigitalObjectManager manager = DigitalObjectManager.getInstance();
        assertThrows(NullPointerException.class, () -> manager.getDigitalObject(null));

        // For null input, the method should fail fast and not touch the DB layer at all
        Mockito.verifyNoInteractions(mockDaoFactory);
    }
}