package cz.inovatika.altoEditor.db.filter;

import java.sql.Timestamp;

/**
 * Represents a filter for querying users. Provides various parameters to
 * customize filtering criteria such as user id, login, ordering, and pagination options.
 */
public final class DigitalObjectFilter {

    public static final int DEFAULT_LIMIT = 1000;
    public static final int DEFAULT_OFFSET = 0;

    private Integer id;
    private Integer rUserId;
    private String login;
    private String instance;
    private String pid;
    private String label;
    private String parentPath;
    private String parentLabel;
    private String version;
    private Timestamp datum;
    private Timestamp datumFrom;
    private Timestamp datumTo;
    private String state;
    private String lock;

    private String orderBy;
    private String orderSort;
    private Integer limit = DEFAULT_LIMIT;
    private Integer offset = DEFAULT_OFFSET;

    public DigitalObjectFilter() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getId() {
        return id;
    }

    public Integer getrUserId() {
        return rUserId;
    }

    public String getLogin() {
        return login;
    }

    public String getInstance() {
        return instance;
    }

    public String getPid() {
        return pid;
    }

    public String getLabel() {
        return label;
    }

    public String getParentPath() {
        return parentPath;
    }

    public String getParentLabel() {
        return parentLabel;
    }

    public String getVersion() {
        return version;
    }

    public Timestamp getDatum() {
        return datum;
    }

    public Timestamp getDatumFrom() {
        return datumFrom;
    }

    public Timestamp getDatumTo() {
        return datumTo;
    }

    public String getState() {
        return state;
    }

    public String getLock() {
        return lock;
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
        private Integer rUserId;
        private String login;
        private String instance;
        private String pid;
        private String label;
        private String parentPath;
        private String parentLabel;
        private String version;
        private Timestamp datum;
        private Timestamp datumFrom;
        private Timestamp datumTo;
        private String state;
        private String lock;
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

        public Builder rUserId(Integer rUserId) {
            this.rUserId = rUserId;
            return this;
        }

        public Builder login(String login) {
            this.login = login;
            return this;
        }

        public Builder instance(String instance) {
            this.instance = instance;
            return this;
        }

        public Builder pid(String pid) {
            this.pid = pid;
            return this;
        }

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder parentPath(String parentPath) {
            this.parentPath = parentPath;
            return this;
        }

        public Builder parentLabel(String parentLabel) {
            this.parentLabel = parentLabel;
            return this;
        }

        public Builder version(String version) {
            this.version = (version != null && !version.startsWith("ALTO.")) ? "ALTO." + version : version;
            return this;
        }

        public Builder datum(Timestamp datum) {
            this.datum = datum;
            return this;
        }

        public Builder datumFrom(Timestamp datumFrom) {
            this.datumFrom = datumFrom;
            return this;
        }

        public Builder datumTo(Timestamp datumTo) {
            this.datumTo = datumTo;
            return this;
        }

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder lock(String lock) {
            this.lock = lock;
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

        public DigitalObjectFilter build() {
            DigitalObjectFilter filter = new DigitalObjectFilter();
            filter.id = this.id;
            filter.rUserId = this.rUserId;
            filter.login = this.login;
            filter.instance = this.instance;
            filter.pid = this.pid;
            filter.label = this.label;
            filter.parentPath = this.parentPath;
            filter.parentLabel = this.parentLabel;
            filter.version = this.version;
            filter.datum = this.datum;
            filter.datumFrom = this.datumFrom;
            filter.datumTo = this.datumTo;
            filter.state = this.state;
            filter.lock = this.lock;
            filter.orderBy = this.orderBy;
            filter.orderSort = this.orderSort;
            filter.limit = this.limit;
            filter.offset = this.offset;
            return filter;
        }
    }
}
