package cz.inovatika.altoEditor.db.filter;

import java.sql.Timestamp;

/**
 * Represents a filter used to apply various constraints or parameters
 * when processing batches of data. This class provides various fields
 * for filtering and sorting data and supports fluent API to customize
 * the filter configuration.
 */
public class BatchFilter {

    private static final int DEFAULT_LIMIT = Integer.MAX_VALUE;
    private static final int DEFAULT_OFFSET = 0;

    private Integer id;
    private String pid;
    private Timestamp createDateFrom;
    private Timestamp createDateTo;
    private Timestamp updateDateFrom;
    private Timestamp updateDateTo;
    private String state;
    private String substate;
    private String priority;
    private String type;
    private String instance;
    private Integer objectId;
    private Integer estimateItemNumber;
    private String log;
    private String orderBy;
    private String orderSort;
    private Integer limit = DEFAULT_LIMIT;
    private Integer offset = DEFAULT_OFFSET;

    public Integer getId() {
        return id;
    }

    public String getPid() {
        return pid;
    }

    public Timestamp getCreateDateFrom() {
        return createDateFrom;
    }

    public Timestamp getCreateDateTo() {
        return createDateTo;
    }

    public Timestamp getUpdateDateFrom() {
        return updateDateFrom;
    }

    public Timestamp getUpdateDateTo() {
        return updateDateTo;
    }

    public String getState() {
        return state;
    }

    public String getSubstate() {
        return substate;
    }

    public String getPriority() {
        return priority;
    }

    public String getType() {
        return type;
    }

    public String getInstance() {
        return instance;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public Integer getEstimateItemNumber() {
        return estimateItemNumber;
    }

    public String getLog() {
        return log;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public String getOrderSort() {
        return orderSort;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getOffset() {
        return offset;
    }

    public BatchFilter setId(Integer id) {
        this.id = id;
        return this;
    }

    public BatchFilter setPid(String pid) {
        this.pid = pid;
        return this;
    }

    public BatchFilter setCreateDateFrom(Timestamp createDateFrom) {
        this.createDateFrom = createDateFrom;
        return this;
    }

    public BatchFilter setCreateDateTo(Timestamp createDateTo) {
        this.createDateTo = createDateTo;
        return this;
    }

    public BatchFilter setUpdateDateFrom(Timestamp updateDateFrom) {
        this.updateDateFrom = updateDateFrom;
        return this;
    }

    public BatchFilter setUpdateDateTo(Timestamp updateDateTo) {
        this.updateDateTo = updateDateTo;
        return this;
    }

    public BatchFilter setState(String state) {
        this.state = state;
        return this;
    }

    public BatchFilter setSubstate(String substate) {
        this.substate = substate;
        return this;
    }

    public BatchFilter setPriority(String priority) {
        this.priority = priority;
        return this;
    }

    public BatchFilter setType(String type) {
        this.type = type;
        return this;
    }

    public BatchFilter setInstance(String instance) {
        this.instance = instance;
        return this;
    }

    public BatchFilter setObjectId(Integer objectId) {
        this.objectId = objectId;
        return this;
    }

    public BatchFilter setEstimateItemNumber(Integer estimateItemNumber) {
        this.estimateItemNumber = estimateItemNumber;
        return this;
    }

    public BatchFilter setLog(String log) {
        this.log = log;
        return this;
    }

    public BatchFilter setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public BatchFilter setOrderSort(String orderSort) {
        this.orderSort = orderSort;
        return this;
    }

    public BatchFilter setLimit(Integer limit) {
        if (limit == null) {
            this.limit = DEFAULT_LIMIT;
            return this;
        }
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be >= 0");
        }
        this.limit = limit;
        return this;
    }

    public BatchFilter setOffset(Integer offset) {
        if (offset == null) {
            this.offset = DEFAULT_OFFSET;
            return this;
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0");
        }
        this.offset = offset;
        return this;
    }
}