import { useCallback, useEffect, useState } from 'react';
import { bookmarksApi, communitiesApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import useAuth from '../hooks/useAuth';
import usePagedResource from '../hooks/usePagedResource';
import { displayName } from '../utils/display';
import { getErrorMessage, joinValues, splitCommaSeparated } from '../utils/http';

const emptyCommunity = { name: '', description: '', resources: '' };

export default function CommunitiesPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ROLE_ADMIN' || user?.role === 'ADMIN';
  const loadCommunities = useCallback((params) => communitiesApi.list(params), []);
  const { page, pageNumber, setPageNumber, setFilters, isLoading, error, reload } = usePagedResource(loadCommunities, { search: '' });
  const [search, setSearch] = useState('');
  const [form, setForm] = useState(emptyCommunity);
  const [editingCommunity, setEditingCommunity] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  function submitSearch(event) {
    event.preventDefault();
    setFilters({ search: search.trim() });
  }

  function openCreateForm() {
    setForm(emptyCommunity);
    setEditingCommunity(null);
    setActionError('');
    setShowForm(true);
  }

  function openEditForm(community) {
    setForm({ name: community.name || '', description: community.description || '', resources: joinValues(community.resources) });
    setEditingCommunity(community);
    setActionError('');
    setShowForm(true);
  }

  async function saveCommunity(event) {
    event.preventDefault();
    setIsSaving(true);
    setActionError('');
    const payload = { ...form, resources: splitCommaSeparated(form.resources) };
    try {
      if (editingCommunity) {
        await communitiesApi.update(editingCommunity.id, payload);
      } else {
        await communitiesApi.create(payload);
      }
      setShowForm(false);
      setNotice(editingCommunity ? 'Community updated.' : 'Community created.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to save the community.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function performAction(action, successMessage) {
    setActionError('');
    setNotice('');
    try {
      await action();
      setNotice(successMessage);
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'That community action could not be completed.'));
    }
  }

  function deleteCommunity(community) {
    if (!window.confirm(`Delete ${community.name}?`)) {
      return;
    }
    performAction(() => communitiesApi.remove(community.id), 'Community deleted.');
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow="Communities"
        title="Skill communities"
        description="Join communities to explore their members, resources, and projects."
        actions={isAdmin ? <button className="button button-primary" onClick={openCreateForm} type="button">Create community</button> : null}
      />
      <form className="list-toolbar" onSubmit={submitSearch}>
        <label className="search-field"><span className="sr-only">Search communities</span><input onChange={(event) => setSearch(event.target.value)} placeholder="Search communities" value={search} /></label>
        <button className="button button-secondary" type="submit">Search</button>
      </form>

      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {notice && <p className="form-message form-message-success" role="status">{notice}</p>}
      {showForm && <CommunityForm editing={Boolean(editingCommunity)} form={form} isSaving={isSaving} onCancel={() => setShowForm(false)} onChange={(event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }))} onSubmit={saveCommunity} />}

      {isLoading ? <InlineLoading message="Loading communities…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No communities found" message="Try a different search." />
      ) : (
        <>
          <section className="card-grid community-card-grid">
            {page.content.map((community) => <CommunityCard community={community} isAdmin={isAdmin} key={community.id} onAction={performAction} onDelete={deleteCommunity} onEdit={openEditForm} />)}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function CommunityCard({ community, isAdmin, onAction, onEdit, onDelete }) {
  const [view, setView] = useState('');
  const [details, setDetails] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [detailError, setDetailError] = useState('');

  useEffect(() => {
    if (!view || view === 'resources') {
      setDetails([]);
      return;
    }
    let active = true;
    setIsLoading(true);
    setDetailError('');
    const request = view === 'members' ? communitiesApi.members(community.id, { page: 0, size: 8 }) : communitiesApi.projects(community.id, { page: 0, size: 8 });
    request.then((response) => {
      if (active) setDetails(response.data?.content || []);
    }).catch((requestError) => {
      if (active) setDetailError(getErrorMessage(requestError, 'Unable to load this community information.'));
    }).finally(() => {
      if (active) setIsLoading(false);
    });
    return () => { active = false; };
  }, [community.id, view]);

  return (
    <article className="content-card community-card">
      <div className="card-heading-row"><h2>{community.name}</h2><span className="status-pill status-muted">Community</span></div>
      <p>{community.description}</p>
      <dl className="community-counts">
        <div><dt>Members</dt><dd>{community.memberCount ?? 0}</dd></div>
        <div><dt>Projects</dt><dd>{community.projectCount ?? 0}</dd></div>
        <div><dt>Resources</dt><dd>{community.resourceCount ?? 0}</dd></div>
      </dl>
      <div className="card-actions card-actions-wrap">
        {community.member ? (
          <button className="button button-secondary button-small" onClick={() => onAction(() => communitiesApi.leave(community.id), 'You left the community.')} type="button">Leave</button>
        ) : (
          <button className="button button-primary button-small" onClick={() => onAction(() => communitiesApi.join(community.id), 'Community joined successfully.')} type="button">Join</button>
        )}
        <button className="button button-secondary button-small" onClick={() => onAction(() => bookmarksApi.create({ targetType: 'COMMUNITY', targetId: community.id }), 'Community bookmarked.')} type="button">Bookmark</button>
      </div>
      <div className="community-view-tabs">
        <button className={view === 'resources' ? 'active' : ''} onClick={() => setView(view === 'resources' ? '' : 'resources')} type="button">View resources</button>
        <button className={view === 'members' ? 'active' : ''} onClick={() => setView(view === 'members' ? '' : 'members')} type="button">View members</button>
        <button className={view === 'projects' ? 'active' : ''} onClick={() => setView(view === 'projects' ? '' : 'projects')} type="button">View projects</button>
      </div>
      {view === 'resources' && <ResourceList resources={community.resources || []} />}
      {view && view !== 'resources' && <CommunityDetails details={details} error={detailError} isLoading={isLoading} type={view} />}
      {isAdmin && <div className="card-actions"><button className="button button-secondary button-small" onClick={() => onEdit(community)} type="button">Edit</button><button className="button button-danger button-small" onClick={() => onDelete(community)} type="button">Delete</button></div>}
    </article>
  );
}

function ResourceList({ resources }) {
  if (!resources.length) return <p className="muted-copy community-details">No resources added.</p>;
  return <ul className="resource-list community-details">{resources.map((resource) => <li key={resource}><a href={resource} rel="noreferrer" target="_blank">{resource}</a></li>)}</ul>;
}

function CommunityDetails({ details, type, isLoading, error }) {
  if (isLoading) return <p className="muted-copy community-details">Loading {type}…</p>;
  if (error) return <p className="field-error community-details">{error}</p>;
  if (!details.length) return <p className="muted-copy community-details">No {type} found.</p>;
  return (
    <ul className="compact-list community-details">
      {details.map((item) => <li key={item.id}>{type === 'members' ? displayName(item) : item.title}</li>)}
    </ul>
  );
}

function CommunityForm({ form, onChange, onSubmit, onCancel, isSaving, editing }) {
  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>{editing ? 'Edit community' : 'Create community'}</h2><p>Use full URLs for community resources.</p></div>
      <label className="field"><span>Community name</span><input maxLength="150" name="name" onChange={onChange} required value={form.name} /></label>
      <label className="field"><span>Description</span><textarea maxLength="3000" name="description" onChange={onChange} required rows="4" value={form.description} /></label>
      <label className="field"><span>Resources (comma separated URLs)</span><textarea name="resources" onChange={onChange} rows="3" value={form.resources} /></label>
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : editing ? 'Save community' : 'Create community'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}
