import { useCallback, useState } from 'react';
import { skillsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import useAuth from '../hooks/useAuth';
import usePagedResource from '../hooks/usePagedResource';
import { humanize } from '../utils/display';
import { getErrorMessage } from '../utils/http';

const emptySkill = {
  name: '',
  level: 'BEGINNER',
  intent: 'TEACH',
  description: '',
  experienceMonths: '',
};

export default function SkillsPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN';
  const loadSkills = useCallback((params) => skillsApi.list(params), []);
  const { page, pageNumber, setPageNumber, setFilters, isLoading, error, reload } = usePagedResource(loadSkills, { search: '' });
  const [search, setSearch] = useState('');
  const [form, setForm] = useState(emptySkill);
  const [editingSkill, setEditingSkill] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  function submitSearch(event) {
    event.preventDefault();
    setFilters({ search: search.trim() });
  }

  function openCreateForm() {
    setEditingSkill(null);
    setForm(emptySkill);
    setFormError('');
    setShowForm(true);
  }

  function openEditForm(skill) {
    setEditingSkill(skill);
    setForm({
      name: skill.name || '',
      level: skill.level || 'BEGINNER',
      intent: skill.intent || 'TEACH',
      description: skill.description || '',
      experienceMonths: skill.experienceMonths ?? '',
    });
    setFormError('');
    setShowForm(true);
  }

  function updateForm(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function saveSkill(event) {
    event.preventDefault();
    setIsSaving(true);
    setFormError('');
    const payload = { ...form, experienceMonths: Number(form.experienceMonths) };

    try {
      if (editingSkill) {
        await skillsApi.update(editingSkill.id, payload);
      } else {
        await skillsApi.create(payload);
      }
      setShowForm(false);
      setForm(emptySkill);
      reload({ page: pageNumber });
    } catch (requestError) {
      setFormError(getErrorMessage(requestError, 'Unable to save the skill.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function deleteSkill(skill) {
    if (!window.confirm(`Delete ${skill.name}?`)) {
      return;
    }

    try {
      await skillsApi.remove(skill.id);
      reload({ page: pageNumber });
    } catch (requestError) {
      setFormError(getErrorMessage(requestError, 'Unable to delete the skill.'));
    }
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow="Skills"
        title="Skills you can teach or want to learn"
        description="Add a level, purpose, description, and experience so other students understand how to collaborate with you."
        actions={<button className="button button-primary" onClick={openCreateForm} type="button">Add skill</button>}
      />

      <form className="list-toolbar" onSubmit={submitSearch}>
        <label className="search-field"><span className="sr-only">Search skills</span><input onChange={(event) => setSearch(event.target.value)} placeholder="Search skills" value={search} /></label>
        <button className="button button-secondary" type="submit">Search</button>
      </form>

      {formError && <p className="form-message form-message-error" role="alert">{formError}</p>}
      {showForm && <SkillForm form={form} isSaving={isSaving} isEditing={Boolean(editingSkill)} onCancel={() => setShowForm(false)} onChange={updateForm} onSubmit={saveSkill} />}

      {isLoading ? <InlineLoading message="Loading skills…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No skills found" message="Add a skill or try a different search." action={<button className="button button-primary" onClick={openCreateForm} type="button">Add skill</button>} />
      ) : (
        <>
          <section className="card-grid skill-card-grid">
            {page.content.map((skill) => {
              const canManage = isAdmin || Number(skill.ownerId) === Number(user?.id);
              return (
                <article className="content-card skill-card" key={skill.id}>
                  <div className="card-heading-row">
                    <div><h2>{skill.name}</h2><p className="muted-copy">{skill.ownerUsername ? `Added by ${skill.ownerUsername}` : 'Your skill'}</p></div>
                    <span className={`status-pill ${skill.intent === 'TEACH' ? 'status-open' : 'status-muted'}`}>{skill.intent === 'TEACH' ? 'Can teach' : 'Want to learn'}</span>
                  </div>
                  <p>{skill.description}</p>
                  <div className="tag-list"><span className="tag">{humanize(skill.level)}</span><span className="tag">{skill.experienceMonths} month{skill.experienceMonths === 1 ? '' : 's'} experience</span></div>
                  {canManage && <div className="card-actions"><button className="button button-secondary button-small" onClick={() => openEditForm(skill)} type="button">Edit</button><button className="button button-danger button-small" onClick={() => deleteSkill(skill)} type="button">Delete</button></div>}
                </article>
              );
            })}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function SkillForm({ form, onChange, onSubmit, onCancel, isSaving, isEditing }) {
  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>{isEditing ? 'Edit skill' : 'Add a skill'}</h2><p>Describe one skill entry at a time.</p></div>
      <div className="form-grid">
        <label className="field"><span>Skill</span><input maxLength="100" name="name" onChange={onChange} required value={form.name} /></label>
        <label className="field"><span>Level</span><select name="level" onChange={onChange} value={form.level}><option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option></select></label>
        <label className="field"><span>Can / want to</span><select name="intent" onChange={onChange} value={form.intent}><option value="TEACH">Can teach</option><option value="LEARN">Want to learn</option></select></label>
        <label className="field"><span>Experience (months)</span><input min="0" name="experienceMonths" onChange={onChange} required type="number" value={form.experienceMonths} /></label>
      </div>
      <label className="field"><span>Description</span><textarea maxLength="1000" name="description" onChange={onChange} required rows="4" value={form.description} /></label>
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : isEditing ? 'Save skill' : 'Add skill'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}
