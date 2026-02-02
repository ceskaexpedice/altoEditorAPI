package cz.inovatika.altoEditor.db.filter;

import java.sql.Timestamp;

/**
 * Represents a filter used to apply various constraints or parameters
 * when processing batches of data. This class provides various fields
 * for filtering and sorting data and supports fluent API to customize
 * the filter configuration.
 */
public class BatchFilter {

    public static final int DEFAULT_LIMIT = 1000;
    public static final int DEFAULT_OFFSET = 0;

    private Integer id;
    private String pid;
    private Timestamp createDateFrom;
    private Timestamp createDateTo;
    private Timestamp createDate;
    private Timestamp updateDateFrom;
    private Timestamp updateDateTo;
    private Timestamp updateDate;
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

    private BatchFilter() {
    }

    public static Builder builder() {
        return new Builder();
    }

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

    public Timestamp getCreateDate() {
        return createDate;
    }

    public Timestamp getUpdateDateFrom() {
        return updateDateFrom;
    }

    public Timestamp getUpdateDateTo() {
        return updateDateTo;
    }

    public Timestamp getUpdateDate() {
        return updateDate;
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

    public static class Builder {
        private Integer id;
        private String pid;
        private Timestamp createDateFrom;
        private Timestamp createDateTo;
        private Timestamp createDate;
        private Timestamp updateDateFrom;
        private Timestamp updateDateTo;
        private Timestamp updateDate;
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

        private Builder() {
        }

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }

        public Builder pid(String pid) {
            this.pid = pid;
            return this;
        }

        public Builder createDateFrom(Timestamp createDateFrom) {
            this.createDateFrom = createDateFrom;
            return this;
        }

        public Builder createDateTo(Timestamp createDateTo) {
            this.createDateTo = createDateTo;
            return this;
        }

        public Builder createDate(Timestamp createDate) {
            this.createDate = createDate;
            return this;
        }

        public Builder updateDateFrom(Timestamp updateDateFrom) {
            this.updateDateFrom = updateDateFrom;
            return this;
        }

        public Builder updateDateTo(Timestamp updateDateTo) {
            this.updateDateTo = updateDateTo;
            return this;
        }

        public Builder updateDate(Timestamp updateDate) {
            this.updateDate = updateDate;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder substate(String substate) {
            this.substate = substate;
            return this;
        }

        public Builder priority(String priority) {
            this.priority = priority;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder instance(String instance) {
            this.instance = instance;
            return this;
        }

        public Builder objectId(Integer objectId) {
            this.objectId = objectId;
            return this;
        }

        public Builder estimateItemNumber(Integer estimateItemNumber) {
            this.estimateItemNumber = estimateItemNumber;
            return this;
        }

        public Builder log(String log) {
            this.log = log;
            return this;
        }

        public Builder orderBy(String orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder orderSort(String orderSort) {
            this.orderSort = orderSort;
            return this;
        }

        public Builder limit(Integer limit) {
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

        public Builder offset(Integer offset) {
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

        public BatchFilter build() {
            BatchFilter filter = new BatchFilter();
            filter.id = this.id;
            filter.pid = this.pid;
            filter.createDateFrom = this.createDateFrom;
            filter.createDateTo = this.createDateTo;
            filter.createDate = this.createDate;
            filter.updateDateFrom = this.updateDateFrom;
            filter.updateDateTo = this.updateDateTo;
            filter.updateDate = this.updateDate;
            filter.state = this.state;
            filter.substate = this.substate;
            filter.priority = this.priority;
            filter.type = this.type;
            filter.instance = this.instance;
            filter.objectId = this.objectId;
            filter.estimateItemNumber = this.estimateItemNumber;
            filter.log = this.log;
            filter.orderBy = this.orderBy;
            filter.orderSort = this.orderSort;
            filter.limit = this.limit;
            filter.offset = this.offset;
            return filter;
        }
    }
}