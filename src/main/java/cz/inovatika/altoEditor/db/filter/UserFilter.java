package cz.inovatika.altoEditor.db.filter;

/**
 * Represents a filter for querying users. Provides various parameters to
 * customize filtering criteria such as user id, login, ordering, and pagination options.
 */
public class UserFilter {

    public static final int DEFAULT_LIMIT = 1000;
    public static final int DEFAULT_OFFSET = 0;

    private Integer id = null;
    private String login = null;

    private String orderBy;
    private String orderSort;
    private Integer limit = DEFAULT_LIMIT;
    private Integer offset = DEFAULT_OFFSET;

    public UserFilter() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public Integer getId() {
        return id;
    }

    public String getLogin() {
        return login;
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

    public UserFilter setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public UserFilter setOrderSort(String orderSort) {
        this.orderSort = orderSort;
        return this;
    }

    public UserFilter setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public UserFilter setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public static class Builder {
        private Integer id;
        private String login;
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

        public Builder login(String login) {
            this.login = login;
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

        public UserFilter build() {
            UserFilter filter = new UserFilter();
            filter.id = this.id;
            filter.login = this.login;
            filter.orderBy = this.orderBy;
            filter.orderSort = this.orderSort;
            filter.limit = this.limit;
            filter.offset = this.offset;
            return filter;
        }
    }
}
