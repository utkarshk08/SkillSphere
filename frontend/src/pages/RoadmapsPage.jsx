import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { roadmapsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import usePagedResource from '../hooks/usePagedResource';
import useAuth from '../hooks/useAuth';
import { getErrorMessage } from '../utils/http';

const emptyRoadmap = { title: '', publicVisible: true, items: [] };

function toForm(roadmap) {
  return {
    title: roadmap.title || '',
    publicVisible: roadmap.publicVisible ?? true,
    items: (roadmap.items || []).map((item) => ({ title: item.title || '', completed: Boolean(item.completed) })),
  };
}

export default function RoadmapsPage() {
  const { isAuthenticated } = useAuth();
  const loadRoadmaps = useCallback((params) => roadmapsApi.mine(params), []);
  const { page, pageNumber, setPageNumber, isLoading, error, reload } = usePagedResource(loadRoadmaps, {}, 8, isAuthenticated);
  const loadPublicRoadmaps = useCallback((params) => roadmapsApi.list(params), []);
  const {
    page: publicPage,
    pageNumber: publicPageNumber,
    setPageNumber: setPublicPageNumber,
    isLoading: isPublicLoading,
    error: publicError,
    reload: reloadPublic,
  } = usePagedResource(loadPublicRoadmaps);
  const [form, setForm] = useState(emptyRoadmap);
  const [editingRoadmap, setEditingRoadmap] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  function openCreateForm() {
    setForm(emptyRoadmap);
    setEditingRoadmap(null);
    setFormError('');
    setShowForm(true);
  }

  function openEditForm(roadmap) {
    setForm(toForm(roadmap));
    setEditingRoadmap(roadmap);
    setFormError('');
    setShowForm(true);
  }

  function addItem() {
    setForm((current) => ({ ...current, items: [...current.items, { title: '', completed: false }] }));
  }

  function changeItem(index, field, value) {
    setForm((current) => ({
      ...current,
      items: current.items.map((item, itemIndex) => itemIndex === index ? { ...item, [field]: value } : item),
    }));
  }

  function removeItem(index) {
    setForm((current) => ({ ...current, items: current.items.filter((_, itemIndex) => itemIndex !== index) }));
  }

  async function saveRoadmap(event) {
    event.preventDefault();
    setIsSaving(true);
    setFormError('');
    const payload = {
      ...form,
      items: form.items.filter((item) => item.title.trim()).map((item) => ({ ...item, title: item.title.trim() })),
    };
    try {
      if (editingRoadmap) {
        await roadmapsApi.update(editingRoadmap.id, payload);
      } else {
        await roadmapsApi.create(payload);
      }
      setShowForm(false);
      reload({ page: pageNumber });
      reloadPublic({ page: publicPageNumber });
    } catch (requestError) {
      setFormError(getErrorMessage(requestError, 'Unable to save the roadmap.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function deleteRoadmap(roadmap) {
    if (!window.confirm(`Delete ${roadmap.title}?`)) return;
    try {
      await roadmapsApi.remove(roadmap.id);
      reload({ page: pageNumber });
      reloadPublic({ page: publicPageNumber });
    } catch (requestError) {
      setFormError(getErrorMessage(requestError, 'Unable to delete the roadmap.'));
    }
  }

  return (
    <AppShell publicView={!isAuthenticated}>
      <PageHeader
        eyebrow={isAuthenticated ? 'Roadmaps' : 'Shared roadmaps'}
        title={isAuthenticated ? 'Your learning roadmaps' : 'Explore shared learning roadmaps'}
        description={isAuthenticated ? 'Create learning modules, mark them complete, and choose whether the roadmap is public. Completion is calculated from your modules.' : 'Browse learning plans that students have chosen to share publicly.'}
        actions={isAuthenticated
          ? <button className="button button-primary" onClick={openCreateForm} type="button">Create roadmap</button>
          : <Link className="button button-primary" to="/login">Sign in to create a roadmap</Link>}
      />
      {formError && <p className="form-message form-message-error" role="alert">{formError}</p>}
      {isAuthenticated && <>
        {showForm && <RoadmapForm form={form} isEditing={Boolean(editingRoadmap)} isSaving={isSaving} onAddItem={addItem} onCancel={() => setShowForm(false)} onChange={(event) => { const { name, value, checked, type } = event.target; setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value })); }} onChangeItem={changeItem} onRemoveItem={removeItem} onSubmit={saveRoadmap} />}
        <section className="section-heading">
          <div><h2>Your roadmaps</h2><p>Private and public roadmaps that you created.</p></div>
        </section>
        {isLoading ? <InlineLoading message="Loading your roadmaps…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
          <EmptyState title="No roadmaps yet" message="Create a roadmap to begin tracking your learning progress." action={<button className="button button-primary" onClick={openCreateForm} type="button">Create roadmap</button>} />
        ) : (
          <>
            <section className="roadmap-list">
              {page.content.map((roadmap) => <RoadmapCard canManage key={roadmap.id} onDelete={deleteRoadmap} onEdit={openEditForm} roadmap={roadmap} />)}
            </section>
            <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
          </>
        )}
      </>}

      {isAuthenticated && <section className="section-heading">
        <div><h2>Shared roadmaps</h2><p>Learning plans other students have chosen to share publicly.</p></div>
      </section>}
      {isPublicLoading ? <InlineLoading message="Loading shared roadmaps…" /> : publicError ? <ErrorState message={publicError} onRetry={reloadPublic} /> : publicPage.content.length === 0 ? (
        <EmptyState title="No shared roadmaps yet" message="Public roadmaps will appear here when students share them." />
      ) : (
        <>
          <section className="roadmap-list">
            {publicPage.content.map((roadmap) => <RoadmapCard key={roadmap.id} roadmap={roadmap} showOwner />)}
          </section>
          <Pagination page={publicPage} pageNumber={publicPageNumber} onPageChange={setPublicPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function RoadmapCard({ roadmap, onEdit, onDelete, canManage = false, showOwner = false }) {
  const items = roadmap.items || [];
  return (
    <article className="content-card roadmap-card">
      <div className="card-heading-row"><div><h2>{roadmap.title}</h2><p className="muted-copy">{showOwner ? `By @${roadmap.ownerUsername || 'student'} · ` : ''}{roadmap.publicVisible ? 'Public roadmap' : 'Private roadmap'}</p></div><strong className="roadmap-progress-label">{roadmap.completionPercentage ?? 0}%</strong></div>
      <div aria-label={`${roadmap.completionPercentage ?? 0}% complete`} className="progress-track"><span style={{ width: `${roadmap.completionPercentage ?? 0}%` }} /></div>
      {items.length ? <ul className="roadmap-item-list">{items.map((item) => <li className={item.completed ? 'completed' : ''} key={item.id || item.title}><span aria-hidden="true">{item.completed ? '✓' : '○'}</span>{item.title}</li>)}</ul> : <p className="muted-copy">No modules added.</p>}
      {canManage && <div className="card-actions"><button className="button button-secondary button-small" onClick={() => onEdit(roadmap)} type="button">Edit</button><button className="button button-danger button-small" onClick={() => onDelete(roadmap)} type="button">Delete</button></div>}
    </article>
  );
}

function RoadmapForm({ form, onChange, onSubmit, onCancel, onAddItem, onChangeItem, onRemoveItem, isSaving, isEditing }) {
  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>{isEditing ? 'Edit roadmap' : 'Create roadmap'}</h2><p>Add each learning module, then check it off as you complete it.</p></div>
      <label className="field"><span>Roadmap title</span><input maxLength="150" name="title" onChange={onChange} required value={form.title} /></label>
      <label className="checkbox-field"><input checked={form.publicVisible} name="publicVisible" onChange={onChange} type="checkbox" /><span>Share this roadmap publicly</span></label>
      <div className="roadmap-form-items">
        <div className="card-heading-row"><h3>Learning modules</h3><button className="button button-secondary button-small" onClick={onAddItem} type="button">Add module</button></div>
        {!form.items.length && <p className="muted-copy">Add modules such as Spring Security, JWT, or OAuth.</p>}
        {form.items.map((item, index) => (
          <div className="roadmap-form-item" key={index}>
            <label className="checkbox-field"><input checked={item.completed} onChange={(event) => onChangeItem(index, 'completed', event.target.checked)} type="checkbox" /><span className="sr-only">Completed</span></label>
            <input aria-label={`Module ${index + 1}`} maxLength="150" onChange={(event) => onChangeItem(index, 'title', event.target.value)} placeholder="Module title" value={item.title} />
            <button className="button button-text button-small" onClick={() => onRemoveItem(index)} type="button">Remove</button>
          </div>
        ))}
      </div>
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : isEditing ? 'Save roadmap' : 'Create roadmap'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}
