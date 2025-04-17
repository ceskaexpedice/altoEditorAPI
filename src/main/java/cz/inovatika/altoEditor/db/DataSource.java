package cz.inovatika.altoEditor.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DataSource {

    private static HikariConfig config = new HikariConfig();

    private static HashMap<String, HikariDataSource> namedDS = new HashMap<>();
    private static Logger LOGGER = LoggerFactory.getLogger(DataSource.class.getName());

    private static String DEFAULT_DS = "default";

    /**
     * inicializace DB spojeni
     * @param dsName  - jemno spojeni at je jich mozne pouzit vice, pokud staci jedno pouzij jinou metodu
     * @param url
     * @param user
     * @param password
     * @param poolSize
     */
    public static void configure(String dsName, String url, String driver, String user, String password, int poolSize) {
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(poolSize);
        if (namedDS.get(dsName) != null) {
            namedDS.get(dsName).close();
        }
        namedDS.put(dsName, new HikariDataSource(config));
    }

    /**
     * inicializace DB spojeni
     * @param url
     * @param user
     * @param password
     * @param poolSize
     */
    public static void configure(String url, String driver, String user, String password, int poolSize) {
        configure(DEFAULT_DS, url, driver, user, password, poolSize);
    }

    /**
     * prekonfigurovani URL
     *
     * @param url
     */
    public static void configure(String url, String driver, String user, String password) {
        configure(url, driver, user, password, 10);
    }

    private DataSource() {
    }

    public static Connection getConnection() throws SQLException {
        return getConnection(DEFAULT_DS);
    }

    public static Connection getConnection(String dsName) throws SQLException {
        if (namedDS.get(dsName) == null) {
            LOGGER.error("Connection not configured ");
            return null;
        }
        return namedDS.get(dsName).getConnection();
    }

    /**
     * spravne nastavi hodnoty dle typu objektu na null
     *
     * @param ps
     * @param pos
     * @param val
     * @throws java.sql.SQLException
     */
    public static void setValue(PreparedStatement ps, int pos, Object val) throws SQLException, NumberFormatException {
//          AddressFinder.logger.info("Delam : {} , {} ",ps.getParameterMetaData().getParameterTypeName(pos) , val);
        switch (ps.getParameterMetaData().getParameterType(pos)) {
            case Types.DATE:
                if (val == null) {
                    ps.setNull(pos, Types.DATE);
                } else {
                    ps.setDate(pos, (Date) val);
                }
                break;
            case Types.TIMESTAMP:
                if (val == null) {
                    ps.setNull(pos, Types.TIMESTAMP);
                } else {
                    ps.setTimestamp(pos, (Timestamp) val);
                }
                break;
            case Types.NUMERIC:
            case Types.INTEGER:
                if (val != null && !val.toString().isEmpty()) {
                    ps.setInt(pos, Integer.parseInt(val.toString()));
                } else {
                    ps.setNull(pos, Types.INTEGER);

                }
                break;
            case Types.BIT:
                if (val != null && Boolean.parseBoolean(val.toString())) {
                    ps.setBoolean(pos, true);
                } else {
                    ps.setBoolean(pos, false);
                }
                break;
            default:
                ps.setString(pos, (String) val);
        }
    }
}

