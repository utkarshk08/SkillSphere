package com.skillsphere.domain;

/** Records the simple action an administrator took to resolve a report. */
public enum AdminReportAction {
    NONE,
    WARNED_USER,
    DELETED_USER,
    DELETED_CONTENT,
    CLOSED_REPORT
}
