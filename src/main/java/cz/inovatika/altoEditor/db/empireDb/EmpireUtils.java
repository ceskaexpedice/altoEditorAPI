package cz.inovatika.altoEditor.db.empireDb;

import java.util.Arrays;
import java.util.List;
import org.apache.empire.db.DBColumnExpr;
import org.apache.empire.db.DBCommand;
import org.apache.empire.db.DBTableColumn;

/**
 * Utility class providing operations that facilitate SQL command manipulation
 * in the context of the Empire database framework. Contains methods for
 * dynamically adding sorting criteria to SQL commands based on given column
 * properties.
 */
class EmpireUtils {

    /**
     * Adds a column to the {@code order by} clause of the command.
     * @param cmd SQL command
     * @param columnBeanPropertyName a property name of the sort column,
     *          possibly prefixed with {@code '-'} to make the sorting descending.
     * @param defaultSortByColumn {@code null} or a column to use in case of
     *          missing {@code columnBeanPropertyName}
     * @param defaultDescending {@code true} to sort {@code defaultSortByColumn} top down
     */
    public static void addOrderBy(DBCommand cmd,
                                  String columnBeanPropertyName, DBTableColumn defaultSortByColumn,
                                  boolean defaultDescending
    ) {
        DBColumnExpr[] selectExprList = cmd.getSelectExprList();
        List<? extends DBColumnExpr> selections = Arrays.asList(selectExprList);
        addOrderBy(cmd, selections, columnBeanPropertyName, defaultSortByColumn, defaultDescending);
    }

    private static void addOrderBy(DBCommand cmd, List<? extends DBColumnExpr> selections,
            String columnBeanPropertyName, DBTableColumn defaultSortByColumn,
            boolean defaultDescending
    ) {
        DBColumnExpr sortByCol = findSelection(selections, columnBeanPropertyName);
        boolean descending;
        if (sortByCol != null) {
            descending = isDescendingSort(columnBeanPropertyName);
        } else if (defaultSortByColumn != null) {
            sortByCol = defaultSortByColumn;
            descending = defaultDescending;
        } else {
            return ;
        }
        cmd.orderBy(sortByCol, descending);
    }

    private static boolean isDescendingSort(String prefixedBeanPropertyName) {
        return prefixedBeanPropertyName.charAt(0) == '-';
    }

    private static DBColumnExpr findSelection(List<? extends DBColumnExpr> selections, String prefixedBeanPropertyName) {
        if (prefixedBeanPropertyName == null) {
            return null;
        }
        String beanPropertyName = prefixedBeanPropertyName.charAt(0) == '-'
                ? prefixedBeanPropertyName.substring(1) : prefixedBeanPropertyName;
        for (DBColumnExpr selection : selections) {
            if (beanPropertyName.equals(selection.getBeanPropertyName())) {
                return selection;
            }
        }
        return null;
    }
}
