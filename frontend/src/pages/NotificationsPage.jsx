import { useCallback, useState } from 'react';
import { notificationsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import usePagedResource from '../hooks/usePagedResource';
import { formatDate, humanize } from '../utils/display';
import { getErrorMessage } from '../utils/http';

const emptyNotification = { type: 'GENERAL', message: '', read: false };

export default function NotificationsPage() {
  const loadNotifications = useCallback((params) => notificationsApi.list(params), []);
  const { page, pageNumber, setPageNumber, isLoading, error, reload } = usePagedResource(loadNotifications);
  const [form, setForm] = useState(emptyNotification);
  const [editingNotification, setEditingNotification] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  function openCreate() {
    setForm(emptyNotification);
    setEditingNotification(null);
    setActionError('');
    setShowForm(true);
  }

  function openEdit(notification) {
    setForm({ type: notification.type, message: notification.message, read: Boolean(notification.read) });
    setEditingNotification(notification);
    setActionError('');
    setShowForm(true);
  }

  async function saveNotification(event) {
    event.preventDefault();
    setIsSaving(true);
    setActionError('');
    try {
      if (editingNotification) {
        await notificationsApi.update(editingNotification.id, form);
      } else {
        await notificationsApi.create({ type: form.type, message: form.message });
      }
      setShowForm(false);
      setNotice(editingNotification ? 'Notification updated.' : 'Notification created.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to save the notification.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function markRead(notification) {
    setActionError('');
    try {
      await notificationsApi.update(notification.id, { type: notification.type, message: notification.message, read: true });
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to mark the notification as read.'));
    }
  }

  async function deleteNotification(notification) {
    if (!window.confirm('Delete this notification?')) return;
    setActionError('');
    try {
      await notificationsApi.remove(notification.id);
      setNotice('Notification deleted.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to delete the notification.'));
    }
  }

  return (
    <AppShell>
      <PageHeader eyebrow="Notifications" title="Your notifications" description="Review collaboration, community, profile, project, and announcement notifications." actions={<button className="button button-primary" onClick={openCreate} type="button">Create notification</button>} />
      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {notice && <p className="form-message form-message-success" role="status">{notice}</p>}
      {showForm && <NotificationForm editing={Boolean(editingNotification)} form={form} isSaving={isSaving} onCancel={() => setShowForm(false)} onChange={(event) => { const { name, value, checked, type } = event.target; setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value })); }} onSubmit={saveNotification} />}
      {isLoading ? <InlineLoading message="Loading notifications…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No notifications" message="New notifications will appear here." />
      ) : (
        <>
          <section className="notification-list">
            {page.content.map((notification) => <NotificationCard key={notification.id} notification={notification} onDelete={deleteNotification} onEdit={openEdit} onMarkRead={markRead} />)}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function NotificationCard({ notification, onEdit, onDelete, onMarkRead }) {
  return (
    <article className={`content-card notification-card${notification.read ? '' : ' unread'}`}>
      <div className="card-heading-row"><div><span className="status-pill status-muted">{humanize(notification.type)}</span><p className="notification-message">{notification.message}</p><p className="muted-copy">{formatDate(notification.createdAt)}</p></div>{!notification.read && <span className="unread-dot" aria-label="Unread" />}</div>
      <div className="card-actions"><button className="button button-secondary button-small" onClick={() => onEdit(notification)} type="button">Edit</button>{!notification.read && <button className="button button-primary button-small" onClick={() => onMarkRead(notification)} type="button">Mark as read</button>}<button className="button button-danger button-small" onClick={() => onDelete(notification)} type="button">Delete</button></div>
    </article>
  );
}

function NotificationForm({ form, onChange, onSubmit, onCancel, isSaving, editing }) {
  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>{editing ? 'Edit notification' : 'Create notification'}</h2><p>Choose the notification type and message.</p></div>
      <label className="field"><span>Type</span><select name="type" onChange={onChange} value={form.type}><option value="GENERAL">General</option><option value="COLLABORATION_REQUEST">Collaboration request</option><option value="COMMUNITY_JOINED">Community joined</option><option value="PROFILE_VERIFIED">Profile verified</option><option value="PROJECT_REQUEST_ACCEPTED">Project request accepted</option><option value="ADMIN_ANNOUNCEMENT">Admin announcement</option></select></label>
      <label className="field"><span>Message</span><textarea maxLength="500" name="message" onChange={onChange} required rows="3" value={form.message} /></label>
      {editing && <label className="checkbox-field"><input checked={form.read} name="read" onChange={onChange} type="checkbox" /><span>Mark as read</span></label>}
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : editing ? 'Save notification' : 'Create notification'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}

