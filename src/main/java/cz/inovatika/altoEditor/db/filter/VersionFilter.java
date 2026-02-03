package cz.inovatika.altoEditor.db.filter;

import java.sql.Timestamp;

/**
 * Represents a filter used for querying or searching based on versioning-related criteria.
 * Provides options to configure filters such as ID, version, date, ordering, offset, and limits.
 * Instances of this class are immutable and can be built using the {@code Builder} inner class.
 */
public final class VersionFilter {

    public static final int DEFAULT_LIMIT = 1000;
    public static final int DEFAULT_OFFSET = 0;

    private Integer id = null;
    private Timestamp datum = null;
    private Timestamp datumFrom = null;
    private Timestamp datumTo = null;
    private Integer version = null;

    private String orderBy;
    private String orderSort;
    private Integer limit = DEFAULT_LIMIT;
    private Integer offset = DEFAULT_OFFSET;

    public VersionFilter() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getId() {
        return id;
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

    public Integer getVersion() {
        return version;
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
        private Timestamp datumFrom = null;
        private Timestamp datumTo = null;
        private Timestamp datum = null;
        private Integer version = null;
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

        public Builder datumFrom(Timestamp datumFrom) {
            this.datumFrom = datumFrom;
            return this;
        }

        public Builder datumTo(Timestamp datumTo) {
            this.datumTo = datumTo;
            return this;
        }

        public Builder datum(Timestamp datum) {
            this.datum = datum;
            return this;
        }

        public Builder version(Integer version) {
            this.version = version;
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

        public VersionFilter build() {
            VersionFilter filter = new VersionFilter();
            filter.id = this.id;
            filter.version = this.version;
            filter.datum = this.datum;
            filter.datumFrom = this.datumFrom;
            filter.datumTo = this.datumTo;
            filter.orderBy = this.orderBy;
            filter.orderSort = this.orderSort;
            filter.limit = this.limit;
            filter.offset = this.offset;
            return filter;
        }
    }
}
