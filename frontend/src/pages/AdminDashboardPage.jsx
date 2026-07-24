import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { adminApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import usePagedResource from '../hooks/usePagedResource';
import useAuth from '../hooks/useAuth';
import { formatDate, humanize } from '../utils/display';
import { getErrorMessage } from '../utils/http';

export default function AdminDashboardPage() {
  const [tab, setTab] = useState('users');
  const { user: currentUser } = useAuth();

  return (
    <AppShell>
      <PageHeader eyebrow="Administration" title="Admin dashboard" description="Manage users, verify public profiles, review reports, manage skills, and publish announcements." actions={<Link className="button button-secondary" to="/skills">Manage skills</Link>} />
      <div className="tab-list" role="tablist" aria-label="Admin sections">
        <button aria-selected={tab === 'users'} className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')} role="tab" type="button">Users</button>
        <button aria-selected={tab === 'reports'} className={tab === 'reports' ? 'active' : ''} onClick={() => setTab('reports')} role="tab" type="button">Reports</button>
        <button aria-selected={tab === 'announcements'} className={tab === 'announcements' ? 'active' : ''} onClick={() => setTab('announcements')} role="tab" type="button">Announcements</button>
      </div>
      {tab === 'users' && <UsersPanel currentUserId={currentUser?.id} />}
      {tab === 'reports' && <ReportsPanel currentUserId={currentUser?.id} />}
      {tab === 'announcements' && <AnnouncementsPanel />}
    </AppShell>
  );
}

function UsersPanel({ currentUserId }) {
  const loadUsers = useCallback((params) => adminApi.users(params), []);
  const { page, pageNumber, setPageNumber, setFilters, isLoading, error, reload } = usePagedResource(loadUsers, { search: '' });
  const [search, setSearch] = useState('');
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');

  function submitSearch(event) {
    event.preventDefault();
    setFilters({ search: search.trim() });
  }

  async function verifyUser(user) {
    setActionError('');
    try {
      await adminApi.verifyProfile(user.id);
      setNotice(`${user.username}'s profile was verified.`);
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to verify this profile.'));
    }
  }

  async function deleteUser(user) {
    if (!window.confirm(`Delete user ${user.username}?`)) return;
    setActionError('');
    try {
      await adminApi.deleteUser(user.id);
      setNotice('User deleted.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to delete this user.'));
    }
  }

  return (
    <section className="admin-section">
      <div className="section-heading"><div><h2>Users</h2><p>Verify public profiles or remove inappropriate user profiles.</p></div></div>
      <form className="list-toolbar" onSubmit={submitSearch}><label className="search-field"><span className="sr-only">Search users</span><input onChange={(event) => setSearch(event.target.value)} placeholder="Search username, email, or college" value={search} /></label><button className="button button-secondary" type="submit">Search</button></form>
      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {notice && <p className="form-message form-message-success" role="status">{notice}</p>}
      {isLoading ? <InlineLoading message="Loading users…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? <EmptyState title="No users found" /> : <>
        <div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>User</th><th>College</th><th>Role</th><th>Profile</th><th>Actions</th></tr></thead><tbody>{page.content.map((user) => <tr key={user.id}><td><strong>{user.fullName || user.username}</strong><span>@{user.username}</span><span>{user.email}</span></td><td>{user.collegeName || '—'}</td><td>{humanize(user.role)}</td><td>{user.verified ? 'Verified' : user.publicProfileVisibility ? 'Public, not verified' : 'Private'}</td><td><div className="table-actions">{!user.verified && <button className="button button-secondary button-small" onClick={() => verifyUser(user)} type="button">Verify</button>}{Number(user.id) !== Number(currentUserId) && <button className="button button-danger button-small" onClick={() => deleteUser(user)} type="button">Delete</button>}</div></td></tr>)}</tbody></table></div>
        <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
      </>}
    </section>
  );
}

function ReportsPanel({ currentUserId }) {
  const loadReports = useCallback((params) => adminApi.reports(params), []);
  const { page, pageNumber, setPageNumber, setFilters, isLoading, error, reload } = usePagedResource(loadReports, { status: '' });
  const [status, setStatus] = useState('');
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');

  function changeStatusFilter(event) {
    const value = event.target.value;
    setStatus(value);
    setFilters({ status: value || undefined });
  }

  async function updateReport(report, adminAction) {
    setActionError('');
    try {
      await adminApi.updateReport(report.id, { status: 'RESOLVED', adminAction });
      setNotice('Report resolution recorded.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to update this report.'));
    }
  }

  async function deleteContent(report) {
    if (!report.reportedContentType || !report.reportedContentId) {
      setActionError('This report does not identify content to delete.');
      return;
    }
    if (!window.confirm('Delete the reported content?')) return;
    setActionError('');
    try {
      await adminApi.deleteContent(report.reportedContentType, report.reportedContentId);
      await adminApi.updateReport(report.id, { status: 'RESOLVED', adminAction: 'DELETED_CONTENT' });
      setNotice('Reported content deleted and report resolved.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to delete the reported content.'));
    }
  }

  async function deleteReportedUser(report) {
    if (!report.reportedUserId) {
      setActionError('This report does not identify a user to delete.');
      return;
    }
    if (!window.confirm('Delete the reported user?')) return;
    setActionError('');
    try {
      await adminApi.deleteUser(report.reportedUserId);
      // Deleting a user also removes reports directly tied to that user to satisfy database FKs.
      setNotice('Reported user and their related report records were deleted.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to delete the reported user.'));
    }
  }

  return (
    <section className="admin-section">
      <div className="section-heading"><div><h2>Reported users and content</h2><p>Review pending reports and record the action taken.</p></div><label className="field compact-select"><span>Status</span><select onChange={changeStatusFilter} value={status}><option value="">All reports</option><option value="PENDING_REVIEW">Pending review</option><option value="RESOLVED">Resolved</option></select></label></div>
      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {notice && <p className="form-message form-message-success" role="status">{notice}</p>}
      {isLoading ? <InlineLoading message="Loading reports…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? <EmptyState title="No reports found" /> : <>
        <section className="report-list admin-report-list">{page.content.map((report) => <article className="content-card report-card" key={report.id}><div className="card-heading-row"><div><span className="status-pill status-muted">{humanize(report.reason)}</span><h3>{report.reportedUsername || (report.reportedContentType ? `${humanize(report.reportedContentType)} #${report.reportedContentId}` : 'Reported item')}</h3><p className="muted-copy">Reported by @{report.reporterUsername} · {formatDate(report.createdAt)}</p></div><span className={`status-pill status-${String(report.status).toLowerCase()}`}>{humanize(report.status)}</span></div>{report.description && <p>{report.description}</p>}{report.status === 'PENDING_REVIEW' && <div className="card-actions card-actions-wrap"><button className="button button-secondary button-small" onClick={() => updateReport(report, 'WARNED_USER')} type="button">Warn user</button><button className="button button-secondary button-small" onClick={() => updateReport(report, 'CLOSED_REPORT')} type="button">Close report</button>{report.reportedUserId && Number(report.reportedUserId) !== Number(currentUserId) && <button className="button button-danger button-small" onClick={() => deleteReportedUser(report)} type="button">Delete user</button>}{report.reportedContentType && <button className="button button-danger button-small" onClick={() => deleteContent(report)} type="button">Delete content</button>}</div>}{report.adminAction && report.adminAction !== 'NONE' && <p className="muted-copy">Action: {humanize(report.adminAction)}</p>}</article>)}</section>
        <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
      </>}
    </section>
  );
}

function AnnouncementsPanel() {
  const loadAnnouncements = useCallback((params) => adminApi.announcements(params), []);
  const { page, pageNumber, setPageNumber, isLoading, error, reload } = usePagedResource(loadAnnouncements);
  const [form, setForm] = useState({ title: '', message: '', active: true });
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  function openCreate() {
    setForm({ title: '', message: '', active: true });
    setEditing(null);
    setActionError('');
    setShowForm(true);
  }

  function openEdit(announcement) {
    setForm({ title: announcement.title, message: announcement.message, active: announcement.active });
    setEditing(announcement);
    setActionError('');
    setShowForm(true);
  }

  async function saveAnnouncement(event) {
    event.preventDefault();
    setIsSaving(true);
    setActionError('');
    try {
      if (editing) await adminApi.updateAnnouncement(editing.id, form);
      else await adminApi.createAnnouncement(form);
      setShowForm(false);
      setNotice(editing ? 'Announcement updated.' : 'Announcement published.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to save the announcement.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function deleteAnnouncement(announcement) {
    if (!window.confirm(`Delete ${announcement.title}?`)) return;
    setActionError('');
    try {
      await adminApi.deleteAnnouncement(announcement.id);
      setNotice('Announcement deleted.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to delete the announcement.'));
    }
  }

  return (
    <section className="admin-section">
      <div className="section-heading"><div><h2>Announcements</h2><p>Publish active announcements for SkillSphere students.</p></div><button className="button button-primary" onClick={openCreate} type="button">Create announcement</button></div>
      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {notice && <p className="form-message form-message-success" role="status">{notice}</p>}
      {showForm && <AnnouncementForm editing={Boolean(editing)} form={form} isSaving={isSaving} onCancel={() => setShowForm(false)} onChange={(event) => { const { name, value, checked, type } = event.target; setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value })); }} onSubmit={saveAnnouncement} />}
      {isLoading ? <InlineLoading message="Loading announcements…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? <EmptyState title="No announcements" /> : <><section className="announcement-list">{page.content.map((announcement) => <article className="content-card announcement-card" key={announcement.id}><div className="card-heading-row"><div><h3>{announcement.title}</h3><p className="muted-copy">By @{announcement.createdByUsername} · {formatDate(announcement.createdAt)}</p></div><span className={`status-pill ${announcement.active ? 'status-open' : 'status-muted'}`}>{announcement.active ? 'Active' : 'Inactive'}</span></div><p>{announcement.message}</p><div className="card-actions"><button className="button button-secondary button-small" onClick={() => openEdit(announcement)} type="button">Edit</button><button className="button button-danger button-small" onClick={() => deleteAnnouncement(announcement)} type="button">Delete</button></div></article>)}</section><Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} /></>}
    </section>
  );
}

function AnnouncementForm({ form, onChange, onSubmit, onCancel, isSaving, editing }) {
  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>{editing ? 'Edit announcement' : 'Create announcement'}</h2><p>Active announcements are delivered as notifications to students.</p></div>
      <label className="field"><span>Title</span><input maxLength="150" name="title" onChange={onChange} required value={form.title} /></label>
      <label className="field"><span>Message</span><textarea maxLength="1000" name="message" onChange={onChange} required rows="4" value={form.message} /></label>
      <label className="checkbox-field"><input checked={form.active} name="active" onChange={onChange} type="checkbox" /><span>Make this announcement active</span></label>
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : editing ? 'Save announcement' : 'Publish announcement'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}
