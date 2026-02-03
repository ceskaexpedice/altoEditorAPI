package cz.inovatika.altoEditor.db.manager;

import cz.inovatika.altoEditor.db.dao.BatchDao;
import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.filter.BatchFilter;
import cz.inovatika.altoEditor.db.model.Batch;
import cz.inovatika.altoEditor.utils.Const;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@code BatchManager} class.
 * This test class contains several test cases to ensure that the behavior of the {@code BatchManager}
 * class is functioning as expected under various scenarios.
 */
class BatchManagerTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field instanceField = BatchManager.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testGetInstance_withoutSetInstance_shouldThrowException() {
        // Arrange
        // No call to setInstance

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, BatchManager::getInstance);
        assertEquals("set instance first!", exception.getMessage());
    }

    @Test
    void testSetInstance_validDaoFactory_shouldInitializeInstance() {
        // Arrange
        DaoFactory daoFactory = mock(DaoFactory.class);

        // Act
        BatchManager.setInstance(daoFactory);
        BatchManager instance = BatchManager.getInstance();

        // Assert
        assertNotNull(instance, "BatchManager instance should not be null after setInstance is called.");
        assertDoesNotThrow(() -> BatchManager.getInstance());
    }

    @Test
    void testSetInstance_nullDaoFactory_shouldThrowException() {
        // Arrange
        DaoFactory daoFactory = null;

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> BatchManager.setInstance(daoFactory));
        assertEquals("daos", exception.getMessage(), "Exception message should indicate that the argument is null.");
    }

    @Test
    void testFinishedWithError_nullBatch_shouldThrowException() {
        // Arrange
        DaoFactory mockDaoFactory = mock(DaoFactory.class);
        BatchManager.setInstance(mockDaoFactory);

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> BatchManager.getInstance().finishedWithError(null, new Exception("Test Exception")));
        assertEquals("batch must not be null", exception.getMessage());
    }

    @Test
    void testStartWaitingBatch_validBatch_shouldUpdateStateToRunning() {
        // Arrange
        Batch mockBatch = mock(Batch.class);
        DaoFactory mockDaoFactory = mock(DaoFactory.class);
        BatchDao mockBatchDao = mock(BatchDao.class);
        Transaction mockTransaction = mock(Transaction.class);

        when(mockDaoFactory.createBatchDao()).thenReturn(mockBatchDao);
        when(mockDaoFactory.createTransaction()).thenReturn(mockTransaction);
        when(mockBatch.getId()).thenReturn(1);
        when(mockBatchDao.findById(1)).thenReturn(Optional.of(mockBatch));

        BatchManager.setInstance(mockDaoFactory);

        // Act
        Batch resultBatch = BatchManager.getInstance().startWaitingBatch(mockBatch);

        // Assert
        verify(mockBatch).setState(Const.BATCH_STATE_RUNNING);
        verify(mockBatchDao).update(mockBatch);
        verify(mockTransaction).commit();
        assertEquals(mockBatch, resultBatch, "Returned batch should match the mock batch.");
    }

    @Test
    void testStartWaitingBatch_nullBatch_shouldThrowException() {
        // Arrange
        DaoFactory mockDaoFactory = mock(DaoFactory.class);
        BatchManager.setInstance(mockDaoFactory);

        // Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> BatchManager.getInstance().startWaitingBatch(null));
        assertEquals("batch must not be null", exception.getMessage());
    }

    @Test
    void testFindWaitingBatches_shouldReturnBatchesInPlannedState() {
        // Arrange
        DaoFactory mockDaoFactory = mock(DaoFactory.class);
        BatchDao mockBatchDao = mock(BatchDao.class);
        Transaction mockTransaction = mock(Transaction.class);
        Batch mockBatch1 = mock(Batch.class);
        Batch mockBatch2 = mock(Batch.class);

        when(mockDaoFactory.createBatchDao()).thenReturn(mockBatchDao);
        when(mockDaoFactory.createTransaction()).thenReturn(mockTransaction);
        when(mockBatchDao.findByFilter(org.mockito.ArgumentMatchers.any(BatchFilter.class)))
        .thenReturn(List.of(mockBatch1, mockBatch2));

        BatchManager.setInstance(mockDaoFactory);

        // Act
        List<Batch> result = BatchManager.getInstance().findWaitingBatches();

        // Assert
        verify(mockTransaction).close();
        assertNotNull(result, "Result should not be null.");
        assertEquals(2, result.size(), "The number of returned batches should match the expected size.");
        assertEquals(List.of(mockBatch1, mockBatch2), result, "Returned batches should match the expected list.");
    }
}