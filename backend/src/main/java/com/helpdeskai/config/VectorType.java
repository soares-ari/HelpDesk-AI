package com.helpdeskai.config;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Custom Hibernate UserType for pgvector.
 * Handles conversion between PGvector and PostgreSQL vector type.
 */
public class VectorType implements UserType<PGvector> {

    @Override
    public int getSqlType() {
        return Types.OTHER;
    }

    @Override
    public Class<PGvector> returnedClass() {
        return PGvector.class;
    }

    @Override
    public boolean equals(PGvector x, PGvector y) {
        return x != null && x.equals(y);
    }

    @Override
    public int hashCode(PGvector x) {
        return x != null ? x.hashCode() : 0;
    }

    @Override
    public PGvector nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        Object value = rs.getObject(position);
        if (value == null) {
            return null;
        }
        if (value instanceof PGvector) {
            return (PGvector) value;
        }
        // If it's a String representation, parse it
        if (value instanceof String) {
            try {
                return new PGvector((String) value);
            } catch (SQLException e) {
                throw new SQLException("Error parsing PGvector from string: " + value, e);
            }
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to PGvector");
    }

    @Override
    public void nullSafeSet(PreparedStatement st, PGvector value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, value);
        }
    }

    @Override
    public PGvector deepCopy(PGvector value) {
        if (value == null) {
            return null;
        }
        // PGvector is immutable, so we can return the same instance
        return new PGvector(value.toArray());
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(PGvector value) {
        return value != null ? value.toString() : null;
    }

    @Override
    public PGvector assemble(Serializable cached, Object owner) {
        if (cached == null) {
            return null;
        }
        try {
            return new PGvector((String) cached);
        } catch (SQLException e) {
            throw new RuntimeException("Error creating PGvector from cached value", e);
        }
    }

    @Override
    public PGvector replace(PGvector detached, PGvector managed, Object owner) {
        return deepCopy(detached);
    }
}
