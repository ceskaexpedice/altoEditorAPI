package cz.inovatika.altoEditor.db.manager;

import cz.inovatika.altoEditor.db.dao.DaoFactory;
import cz.inovatika.altoEditor.db.dao.Transaction;
import cz.inovatika.altoEditor.db.dao.UserDao;
import cz.inovatika.altoEditor.db.model.User;
import java.lang.reflect.Field;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test class for {@link UserManager#setInstance(DaoFactory)}.
 * <p>
 * This class includes unit tests to ensure the correct behavior of the
 * setInstance method, which initializes a singleton instance of UserManager.
 */
class UserManagerTest {

    @BeforeEach
    void resetSingleton() throws Exception {
        Field instanceField = UserManager.class.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void testSetInstance_withValidDaoFactory_shouldNotThrow() {
        // Arrange
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);

        // Act & Assert
        assertDoesNotThrow(() -> UserManager.setInstance(mockDaoFactory));
    }

    @Test
    void testSetInstance_withNullDaoFactory_shouldThrowNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> UserManager.setInstance(null), "daos");
    }

    @Test
    void testGetInstance_withoutSettingInstance_shouldThrowIllegalStateException() {

        // Act & Assert
        assertThrows(IllegalStateException.class, UserManager::getInstance, "set instance first!");
    }

    @Test
    void testUpdateUser_withValidUser_shouldCommitAndReturnUpdatedUser() {
        // Arrange
        User validUser = new User();
        validUser.setId(1);
        validUser.setLogin("testUser");

        UserDao mockUserDao = Mockito.mock(UserDao.class);
        Transaction mockTransaction = Mockito.mock(Transaction.class);
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);

        Mockito.when(mockDaoFactory.createUserDao()).thenReturn(mockUserDao);
        Mockito.when(mockDaoFactory.createTransaction()).thenReturn(mockTransaction);
        Mockito.when(mockUserDao.findById(1)).thenReturn(java.util.Optional.of(validUser));

        UserManager.setInstance(mockDaoFactory);

        // Act
        User updatedUser = UserManager.getInstance().updateUser(validUser);

        // Assert
        Mockito.verify(mockTransaction).commit();
        Mockito.verify(mockTransaction).close();
        Mockito.verify(mockUserDao).setTransaction(mockTransaction);
        Mockito.verify(mockUserDao).update(validUser);
        assert updatedUser == validUser;
    }

    @Test
    void testUpdateUser_withNullUser_shouldThrowNullPointerException() {
        // Arrange
        DaoFactory mockDaoFactory = Mockito.mock(DaoFactory.class);
        UserManager.setInstance(mockDaoFactory);

        // Assert
        assertThrows(NullPointerException.class, () -> UserManager.getInstance().updateUser(null), "user must not be null");
    }
}