package com.skillsphere.domain;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationMappingTest {

    @Test
    void usesDatabaseSafeColumnNameForReadStatus() throws NoSuchFieldException {
        Field readField = Notification.class.getDeclaredField("read");
        Column column = readField.getAnnotation(Column.class);

        assertEquals("is_read", column.name());
    }
}
