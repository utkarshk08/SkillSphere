import { useCallback, useState } from 'react';
import { bookmarksApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import usePagedResource from '../hooks/usePagedResource';
import { formatDate, mediaUrl } from '../utils/display';
import { getErrorMessage } from '../utils/http';

export default function BookmarksPage() {
  const loadBookmarks = useCallback((params) => bookmarksApi.list(params), []);
  const { page, pageNumber, setPageNumber, isLoading, error, reload } = usePagedResource(loadBookmarks);
  const [actionError, setActionError] = useState('');
  const [editingBookmark, setEditingBookmark] = useState(null);
  const [form, setForm] = useState({ targetType: 'PROFILE', targetId: '' });
  const [isSaving, setIsSaving] = useState(false);

  function openEdit(bookmark) {
    setEditingBookmark(bookmark);
    setForm({ targetType: bookmark.targetType, targetId: String(bookmark.targetId) });
    setActionError('');
  }

  async function updateBookmark(event) {
    event.preventDefault();
    setIsSaving(true);
    setActionError('');
    try {
      await bookmarksApi.update(editingBookmark.id, { targetType: form.targetType, targetId: Number(form.targetId) });
      setEditingBookmark(null);
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to update the bookmark.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function removeBookmark(bookmark) {
    if (!window.confirm(`Remove ${bookmark.targetName || 'this bookmark'}?`)) return;
    setActionError('');
    try {
      await bookmarksApi.remove(bookmark.id);
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to remove the bookmark.'));
    }
  }

  return (
    <AppShell>
      <PageHeader eyebrow="Bookmarks" title="Saved profiles and communities" description="Bookmarks help you return to students and communities you want to explore later." />
      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {editingBookmark && <BookmarkForm form={form} isSaving={isSaving} onCancel={() => setEditingBookmark(null)} onChange={(event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }))} onSubmit={updateBookmark} />}
      {isLoading ? <InlineLoading message="Loading bookmarks…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No bookmarks yet" message="Bookmark a student profile or community while you browse." />
      ) : (
        <>
          <section className="bookmark-list">
            {page.content.map((bookmark) => <BookmarkCard bookmark={bookmark} key={bookmark.id} onEdit={openEdit} onRemove={removeBookmark} />)}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function BookmarkCard({ bookmark, onEdit, onRemove }) {
  const image = mediaUrl(bookmark.targetImage);
  return (
    <article className="content-card bookmark-card">
      <div className="bookmark-target-image">{image ? <img alt="" src={image} /> : <span>{bookmark.targetType === 'PROFILE' ? 'P' : 'C'}</span>}</div>
      <div className="bookmark-details">
        <span className="status-pill status-muted">{bookmark.targetType === 'PROFILE' ? 'Profile' : 'Community'}</span>
        <h2>{bookmark.targetName || 'Saved item'}</h2>
        <p className="muted-copy">Saved {formatDate(bookmark.createdAt)}</p>
      </div>
      <div className="bookmark-actions"><button className="button button-secondary button-small" onClick={() => onEdit(bookmark)} type="button">Edit</button><button className="button button-danger button-small" onClick={() => onRemove(bookmark)} type="button">Remove</button></div>
    </article>
  );
}

function BookmarkForm({ form, onChange, onSubmit, onCancel, isSaving }) {
  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>Edit bookmark</h2><p>Change the saved profile or community target.</p></div>
      <div className="form-grid">
        <label className="field"><span>Target type</span><select name="targetType" onChange={onChange} value={form.targetType}><option value="PROFILE">Profile</option><option value="COMMUNITY">Community</option></select></label>
        <label className="field"><span>Target ID</span><input min="1" name="targetId" onChange={onChange} required type="number" value={form.targetId} /></label>
      </div>
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : 'Save bookmark'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}
