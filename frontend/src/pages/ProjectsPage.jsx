import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { collaborationRequestsApi, communitiesApi, projectsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import useAuth from '../hooks/useAuth';
import usePagedResource from '../hooks/usePagedResource';
import { formatDate, humanize, mediaUrl } from '../utils/display';
import { getErrorMessage, joinValues, splitCommaSeparated } from '../utils/http';

const emptyProject = {
  title: '',
  description: '',
  githubLink: '',
  techStack: '',
  requiredSkills: '',
  deadline: '',
  maximumMembers: '',
  status: 'OPEN',
  difficultyLevel: 'BEGINNER',
  communityId: '',
};

function projectForm(project) {
  return {
    title: project.title || '',
    description: project.description || '',
    githubLink: project.githubLink || '',
    techStack: joinValues(project.techStack),
    requiredSkills: joinValues(project.requiredSkills),
    deadline: project.deadline || '',
    maximumMembers: project.maximumMembers ?? '',
    status: project.status || 'OPEN',
    difficultyLevel: project.difficultyLevel || 'BEGINNER',
    communityId: project.communityId ? String(project.communityId) : '',
  };
}

export default function ProjectsPage() {
  const { user } = useAuth();
  const loadProjects = useCallback((params) => projectsApi.list(params), []);
  const { page, pageNumber, setPageNumber, setFilters, isLoading, error, reload } = usePagedResource(loadProjects, { search: '' });
  const [search, setSearch] = useState('');
  const [form, setForm] = useState(emptyProject);
  const [images, setImages] = useState([]);
  const [editingProject, setEditingProject] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [formError, setFormError] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const [communities, setCommunities] = useState([]);

  useEffect(() => {
    communitiesApi.list({ page: 0, size: 100 })
      .then((response) => setCommunities(response.data?.content || []))
      .catch(() => setCommunities([]));
  }, []);

  function submitSearch(event) {
    event.preventDefault();
    setFilters({ search: search.trim() });
  }

  function openCreateForm() {
    setEditingProject(null);
    setForm(emptyProject);
    setImages([]);
    setFormError('');
    setShowForm(true);
  }

  function openEditForm(project) {
    setEditingProject(project);
    setForm(projectForm(project));
    setImages([]);
    setFormError('');
    setShowForm(true);
  }

  function updateForm(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  }

  async function saveProject(event) {
    event.preventDefault();
    setIsSaving(true);
    setFormError('');
    const payload = {
      ...form,
      githubLink: form.githubLink.trim() || null,
      techStack: splitCommaSeparated(form.techStack),
      requiredSkills: splitCommaSeparated(form.requiredSkills),
      maximumMembers: Number(form.maximumMembers),
      communityId: form.communityId ? Number(form.communityId) : null,
    };
    let savedProject = null;
    let uploadedImageCount = 0;

    try {
      const response = editingProject
        ? await projectsApi.update(editingProject.id, payload)
        : await projectsApi.create(payload);
      savedProject = response.data || editingProject;
      const projectId = savedProject?.id;

      if (images.length && projectId) {
        // Upload in order so a retry only retains the files that have not yet been stored.
        for (const file of images) {
          await projectsApi.uploadImage(projectId, file);
          uploadedImageCount += 1;
        }
      }
      setShowForm(false);
      setImages([]);
      setForm(emptyProject);
      reload({ page: pageNumber });
    } catch (requestError) {
      if (savedProject) {
        setEditingProject(savedProject);
        setForm(projectForm(savedProject));
        setImages((current) => current.slice(uploadedImageCount));
      }
      setFormError(getErrorMessage(
        requestError,
        savedProject
          ? 'Project details were saved, but an image could not be uploaded. You can correct it and try again.'
          : 'Unable to save the project.',
      ));
    } finally {
      setIsSaving(false);
    }
  }

  async function deleteProject(project) {
    if (!window.confirm(`Delete ${project.title}?`)) {
      return;
    }
    try {
      await projectsApi.remove(project.id);
      reload({ page: pageNumber });
    } catch (requestError) {
      setFormError(getErrorMessage(requestError, 'Unable to delete the project.'));
    }
  }

  return (
    <AppShell>
      <PageHeader
        eyebrow="Projects"
        title="Student project collaborations"
        description="Create a mini-project listing with the skills, deadline, team capacity, status, and difficulty level it needs."
        actions={<button className="button button-primary" onClick={openCreateForm} type="button">Create project</button>}
      />
      <form className="list-toolbar" onSubmit={submitSearch}>
        <label className="search-field"><span className="sr-only">Search projects</span><input onChange={(event) => setSearch(event.target.value)} placeholder="Search projects" value={search} /></label>
        <button className="button button-secondary" type="submit">Search</button>
      </form>

      {formError && <p className="form-message form-message-error" role="alert">{formError}</p>}
      {showForm && <ProjectForm communities={communities} editing={Boolean(editingProject)} files={images} form={form} isSaving={isSaving} onCancel={() => setShowForm(false)} onChange={updateForm} onFilesChange={(event) => setImages(Array.from(event.target.files || []))} onSubmit={saveProject} />}

      {isLoading ? <InlineLoading message="Loading projects…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No projects found" message="Create a project or try a different search." action={<button className="button button-primary" onClick={openCreateForm} type="button">Create project</button>} />
      ) : (
        <>
          <section className="card-grid project-card-grid">
            {page.content.map((project) => <ProjectCard key={project.id} onDelete={deleteProject} onEdit={openEditForm} project={project} user={user} />)}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function ProjectCard({ project, onEdit, onDelete, user }) {
  const [showApplicationForm, setShowApplicationForm] = useState(false);
  const [applicationMessage, setApplicationMessage] = useState('');
  const [applicationError, setApplicationError] = useState('');
  const [applicationSent, setApplicationSent] = useState(false);
  const [isApplying, setIsApplying] = useState(false);
  const images = Array.isArray(project.projectImages) ? project.projectImages : [];
  const members = Array.isArray(project.members) ? project.members : [];
  const isOwner = Number(project.ownerId) === Number(user?.id)
    || (project.ownerUsername && project.ownerUsername === user?.username);
  const isMember = members.some((member) => (
    Number(member.id) === Number(user?.id)
    || (member.username && member.username === user?.username)
  ));
  const isOpen = project.status === 'OPEN';
  const hasOpenPosition = Number(project.openPositions) > 0;
  const canApply = Boolean(user) && !isOwner && !isMember && isOpen && hasOpenPosition && !applicationSent;

  async function applyToJoin(event) {
    event.preventDefault();
    setApplicationError('');
    setIsApplying(true);

    try {
      await collaborationRequestsApi.create({
        projectId: project.id,
        message: applicationMessage.trim(),
      });
      setApplicationMessage('');
      setShowApplicationForm(false);
      setApplicationSent(true);
    } catch (requestError) {
      setApplicationError(getErrorMessage(requestError, 'Unable to send this project application.'));
    } finally {
      setIsApplying(false);
    }
  }

  return (
    <article className="content-card project-card">
      {images[0] && <img alt="Project" className="project-cover" src={mediaUrl(images[0])} />}
      <div className="card-heading-row">
        <div>
          <h2>{project.title}</h2>
          <p className="muted-copy">
            By{' '}
            {project.ownerUsername
              ? <Link className="inline-profile-link" to={`/profiles/${project.ownerUsername}`}>@{project.ownerUsername}</Link>
              : 'Student'}
          </p>
        </div>
        <span className={`status-pill status-${String(project.status || '').toLowerCase()}`}>{humanize(project.status)}</span>
      </div>
      <p>{project.description}</p>
      <dl className="compact-details">
        <div><dt>Deadline</dt><dd>{formatDate(project.deadline)}</dd></div>
        <div><dt>Difficulty</dt><dd>{humanize(project.difficultyLevel)}</dd></div>
        <div><dt>Members</dt><dd>{project.currentMemberCount ?? 0} / {project.maximumMembers}</dd></div>
        <div><dt>Open positions</dt><dd>{project.openPositions ?? 0}</dd></div>
      </dl>
      <p className="card-label">Tech stack</p><div className="tag-list">{(project.techStack || []).map((item) => <span className="tag" key={item}>{item}</span>)}</div>
      <p className="card-label">Required skills</p><div className="tag-list">{(project.requiredSkills || []).map((item) => <span className="tag" key={item}>{item}</span>)}</div>
      <div className="project-members">
        <p className="card-label">Team members</p>
        {members.length > 0 ? (
          <div className="member-link-list">
            {members.map((member) => (
              member.username ? (
                <Link className="member-link" key={member.id || member.username} to={`/profiles/${member.username}`}>
                  {member.fullName || `@${member.username}`}
                </Link>
              ) : (
                <span className="member-link" key={member.id}>{member.fullName || 'Student'}</span>
              )
            ))}
          </div>
        ) : <p className="muted-copy">No team members are listed yet.</p>}
      </div>
      {project.githubLink && <a className="text-link" href={project.githubLink} rel="noreferrer" target="_blank">View GitHub repository</a>}

      {!isOwner && (
        <div className="project-application">
          {isMember ? (
            <p className="form-message form-message-success">You are a member of this project.</p>
          ) : applicationSent ? (
            <p className="form-message form-message-success" role="status">Application sent to the project owner.</p>
          ) : !isOpen ? (
            <p className="muted-copy">Applications are unavailable while this project is {humanize(project.status).toLowerCase()}.</p>
          ) : !hasOpenPosition ? (
            <p className="muted-copy">This project team is full.</p>
          ) : showApplicationForm ? (
            <form className="project-application-form" onSubmit={applyToJoin}>
              <label className="field">
                <span>Message to the project owner</span>
                <textarea
                  maxLength="1000"
                  onChange={(event) => setApplicationMessage(event.target.value)}
                  placeholder="Introduce yourself and explain how you can contribute."
                  required
                  rows="3"
                  value={applicationMessage}
                />
                <small>{applicationMessage.length}/1000 characters</small>
              </label>
              {applicationError && <p className="form-message form-message-error" role="alert">{applicationError}</p>}
              <div className="form-actions">
                <button className="button button-primary button-small" disabled={isApplying} type="submit">{isApplying ? 'Sending…' : 'Send application'}</button>
                <button className="button button-secondary button-small" disabled={isApplying} onClick={() => { setShowApplicationForm(false); setApplicationError(''); }} type="button">Cancel</button>
              </div>
            </form>
          ) : canApply ? (
            <button className="button button-primary button-small" onClick={() => setShowApplicationForm(true)} type="button">Apply to join</button>
          ) : null}
        </div>
      )}

      {isOwner && <div className="card-actions"><button className="button button-secondary button-small" onClick={() => onEdit(project)} type="button">Edit</button><button className="button button-danger button-small" onClick={() => onDelete(project)} type="button">Delete</button></div>}
    </article>
  );
}

function ProjectForm({ form, files, communities, onChange, onSubmit, onCancel, onFilesChange, isSaving, editing }) {
  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>{editing ? 'Edit project' : 'Create a project'}</h2><p>Fill in the specified project details.</p></div>
      <div className="form-grid">
        <label className="field"><span>Title</span><input maxLength="150" name="title" onChange={onChange} required value={form.title} /></label>
        <label className="field"><span>GitHub link</span><input name="githubLink" onChange={onChange} type="url" value={form.githubLink} /></label>
        <label className="field"><span>Tech stack (comma separated)</span><input name="techStack" onChange={onChange} required value={form.techStack} /></label>
        <label className="field"><span>Required skills (comma separated)</span><input name="requiredSkills" onChange={onChange} required value={form.requiredSkills} /></label>
        <label className="field"><span>Deadline</span><input name="deadline" onChange={onChange} required type="date" value={form.deadline} /></label>
        <label className="field"><span>Maximum members</span><input max="20" min="1" name="maximumMembers" onChange={onChange} required type="number" value={form.maximumMembers} /></label>
        <label className="field"><span>Status</span><select name="status" onChange={onChange} value={form.status}><option value="OPEN">Open</option><option value="CLOSED">Closed</option><option value="IN_PROGRESS">In progress</option><option value="COMPLETED">Completed</option></select></label>
        <label className="field"><span>Difficulty level</span><select name="difficultyLevel" onChange={onChange} value={form.difficultyLevel}><option value="BEGINNER">Beginner</option><option value="INTERMEDIATE">Intermediate</option><option value="ADVANCED">Advanced</option></select></label>
        <label className="field"><span>Community (optional)</span><select name="communityId" onChange={onChange} value={form.communityId}><option value="">No community</option>{communities.map((community) => <option key={community.id} value={community.id}>{community.name}</option>)}</select></label>
      </div>
      <label className="field"><span>Description</span><textarea maxLength="3000" name="description" onChange={onChange} required rows="4" value={form.description} /></label>
      <label className="field"><span>Project images</span><input accept="image/jpeg,image/png,image/webp" multiple onChange={onFilesChange} type="file" /><small>JPG, PNG, or WEBP files. {files.length ? `${files.length} selected.` : ''}</small></label>
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : editing ? 'Save project' : 'Create project'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}
