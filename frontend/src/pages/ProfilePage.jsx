import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { profileApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import useAuth from '../hooks/useAuth';
import { displayName, initials, mediaUrl } from '../utils/display';
import { getErrorMessage, joinValues, splitCommaSeparated } from '../utils/http';

function toForm(profile = {}) {
  return {
    firstName: profile.firstName || '',
    lastName: profile.lastName || '',
    username: profile.username || '',
    email: profile.email || '',
    collegeName: profile.collegeName || profile.college || '',
    course: profile.course || '',
    yearOfStudy: profile.yearOfStudy || profile.year || '',
    country: profile.country || '',
    githubUrl: profile.githubUrl || '',
    linkedinUrl: profile.linkedinUrl || '',
    portfolioUrl: profile.portfolioUrl || '',
    bio: profile.bio || '',
    interests: joinValues(profile.interests),
    publicProfileVisibility: profile.publicProfileVisibility ?? true,
  };
}

function valueList(value) {
  if (Array.isArray(value)) {
    return value;
  }

  return typeof value === 'string' ? value.split(',').map((item) => item.trim()).filter(Boolean) : [];
}

export default function ProfilePage() {
  const { user, logout, refreshCurrentUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState(() => toForm(user));
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [pictureFile, setPictureFile] = useState(null);
  const [isUploading, setIsUploading] = useState(false);
  const navigate = useNavigate();

  async function loadProfile() {
    setIsLoading(true);
    setError('');
    try {
      const response = await profileApi.getMine();
      setProfile(response.data);
      setForm(toForm(response.data));
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to load your profile.'));
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadProfile();
  }, []);

  const teachingSkills = useMemo(() => valueList(profile?.teachingSkills || profile?.canTeachSkills), [profile]);
  const learningSkills = useMemo(() => valueList(profile?.currentLearningSkills || profile?.learningSkills || profile?.wantToLearnSkills), [profile]);

  function updateField(event) {
    const { name, value, checked, type } = event.target;
    setForm((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }));
  }

  async function saveProfile(event) {
    event.preventDefault();
    setIsSaving(true);
    setError('');
    setMessage('');

    try {
      const response = await profileApi.updateMine({
        ...form,
        interests: splitCommaSeparated(form.interests),
      });
      const updated = response.data || { ...profile, ...form };
      setProfile(updated);
      setForm(toForm(updated));
      setIsEditing(false);
      setMessage('Your profile was updated.');
      await refreshCurrentUser();
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to update your profile.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function uploadPicture(event) {
    event.preventDefault();
    if (!pictureFile) {
      return;
    }

    setIsUploading(true);
    setError('');
    setMessage('');
    try {
      await profileApi.uploadPicture(pictureFile);
      setPictureFile(null);
      setMessage('Your profile picture was uploaded.');
      await loadProfile();
      await refreshCurrentUser();
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to upload the profile picture.'));
    } finally {
      setIsUploading(false);
    }
  }

  async function deleteProfile() {
    if (!window.confirm('Delete your SkillSphere profile? This action cannot be undone.')) {
      return;
    }

    setError('');
    try {
      await profileApi.deleteMine();
      logout();
      navigate('/');
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to delete your profile.'));
    }
  }

  if (isLoading) {
    return <InlineLoading message="Loading your profile…" />;
  }

  if (error && !profile) {
    return <AppShell><ErrorState message={error} onRetry={loadProfile} /></AppShell>;
  }

  if (!profile) {
    return <AppShell><EmptyState title="Your profile is not available" message="Please try again later." /></AppShell>;
  }

  const photoUrl = mediaUrl(profile.profilePicturePath);
  const name = displayName(profile);

  return (
    <AppShell>
      <PageHeader
        eyebrow="My profile"
        title={name}
        description="Keep the information other students use to find and collaborate with you up to date."
        actions={!isEditing && <button className="button button-primary" onClick={() => setIsEditing(true)} type="button">Edit profile</button>}
      />

      {error && <p className="form-message form-message-error" role="alert">{error}</p>}
      {message && <p className="form-message form-message-success" role="status">{message}</p>}

      <section className="profile-layout">
        <aside className="profile-summary-card">
          <div className="profile-avatar profile-avatar-large">
            {photoUrl ? <img alt={`${name}'s profile`} src={photoUrl} /> : <span>{initials(profile)}</span>}
          </div>
          <h2>{name}</h2>
          <p className="profile-username">@{profile.username}</p>
          <p>{profile.collegeName || profile.college || 'College not added'}</p>
          <div className="status-row">
            <span className={`status-pill ${profile.publicProfileVisibility ? 'status-open' : 'status-muted'}`}>
              {profile.publicProfileVisibility ? 'Public profile' : 'Private profile'}
            </span>
            {profile.verified && <span className="status-pill status-verified">Verified</span>}
          </div>

          <form className="picture-form" onSubmit={uploadPicture}>
            <label className="file-label" htmlFor="profile-picture">Profile picture</label>
            <input accept="image/jpeg,image/png,image/webp" id="profile-picture" onChange={(event) => setPictureFile(event.target.files?.[0] || null)} type="file" />
            <button className="button button-secondary button-small" disabled={!pictureFile || isUploading} type="submit">
              {isUploading ? 'Uploading…' : 'Upload picture'}
            </button>
          </form>
        </aside>

        <div className="profile-content">
          {isEditing ? (
            <ProfileForm
              form={form}
              isSaving={isSaving}
              onCancel={() => { setForm(toForm(profile)); setIsEditing(false); }}
              onChange={updateField}
              onSubmit={saveProfile}
            />
          ) : (
            <ProfileDetails learningSkills={learningSkills} profile={profile} teachingSkills={teachingSkills} />
          )}

          {!isEditing && (
            <section className="danger-zone">
              <h2>Delete profile</h2>
              <p>Delete your SkillSphere profile if you no longer want to use the platform.</p>
              <button className="button button-danger" onClick={deleteProfile} type="button">Delete profile</button>
            </section>
          )}
        </div>
      </section>
    </AppShell>
  );
}

function ProfileDetails({ profile, teachingSkills, learningSkills }) {
  const links = [
    ['GitHub', profile.githubUrl],
    ['LinkedIn', profile.linkedinUrl],
    ['Portfolio', profile.portfolioUrl],
  ].filter(([, value]) => value);

  return (
    <>
      <section className="content-card profile-details-card">
        <h2>About</h2>
        <p>{profile.bio || 'No bio added yet.'}</p>
        <dl className="detail-list">
          <div><dt>Email</dt><dd>{profile.email || 'Not added'}</dd></div>
          <div><dt>College</dt><dd>{profile.collegeName || profile.college || 'Not added'}</dd></div>
          <div><dt>Course</dt><dd>{profile.course || 'Not added'}</dd></div>
          <div><dt>Year</dt><dd>{profile.yearOfStudy || profile.year || 'Not added'}</dd></div>
          <div><dt>Country</dt><dd>{profile.country || 'Not added'}</dd></div>
          <div><dt>Projects count</dt><dd>{profile.projectsCount ?? 0}</dd></div>
        </dl>
        {links.length > 0 && (
          <div className="external-links">
            {links.map(([label, url]) => <a href={url} key={label} rel="noreferrer" target="_blank">{label}</a>)}
          </div>
        )}
      </section>

      <section className="skill-summary-grid">
        <SkillSummary title="Teaching skills" values={teachingSkills} emptyMessage="No teaching skills added." />
        <SkillSummary title="Current learning skills" values={learningSkills} emptyMessage="No learning skills added." />
        <SkillSummary title="Interests" values={valueList(profile.interests)} emptyMessage="No interests added." />
      </section>
    </>
  );
}

function SkillSummary({ title, values, emptyMessage }) {
  return (
    <section className="content-card">
      <h2>{title}</h2>
      {values.length ? <div className="tag-list">{values.map((value) => <span className="tag" key={value}>{value}</span>)}</div> : <p className="muted-copy">{emptyMessage}</p>}
    </section>
  );
}

function ProfileForm({ form, onChange, onSubmit, onCancel, isSaving }) {
  return (
    <form className="content-card profile-form" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>Edit profile</h2><p>Username and email are sign-in identifiers, so they stay unchanged here.</p></div>
      <div className="form-grid">
        <FormField label="First name" name="firstName" onChange={onChange} required value={form.firstName} />
        <FormField label="Last name" name="lastName" onChange={onChange} required value={form.lastName} />
        <FormField disabled label="Username" name="username" onChange={onChange} required value={form.username} />
        <FormField disabled label="Email" name="email" onChange={onChange} required type="email" value={form.email} />
        <FormField label="College" name="collegeName" onChange={onChange} required value={form.collegeName} />
        <FormField label="Course" name="course" onChange={onChange} required value={form.course} />
        <FormField label="Year" name="yearOfStudy" onChange={onChange} required value={form.yearOfStudy} />
        <FormField label="Country" name="country" onChange={onChange} required value={form.country} />
        <FormField label="GitHub URL" name="githubUrl" onChange={onChange} type="url" value={form.githubUrl} />
        <FormField label="LinkedIn URL" name="linkedinUrl" onChange={onChange} type="url" value={form.linkedinUrl} />
        <FormField label="Portfolio URL" name="portfolioUrl" onChange={onChange} type="url" value={form.portfolioUrl} />
        <FormField label="Interests (comma separated)" name="interests" onChange={onChange} value={form.interests} />
      </div>
      <label className="field"><span>Bio</span><textarea name="bio" onChange={onChange} rows="4" value={form.bio} /></label>
      <label className="checkbox-field"><input checked={form.publicProfileVisibility} name="publicProfileVisibility" onChange={onChange} type="checkbox" /><span>Make my profile public</span></label>
      <div className="form-actions">
        <button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : 'Save changes'}</button>
        <button className="button button-secondary" onClick={onCancel} type="button">Cancel</button>
      </div>
    </form>
  );
}

function FormField({ label, name, value, onChange, type = 'text', required, disabled }) {
  return (
    <label className="field"><span>{label}</span><input disabled={disabled} name={name} onChange={onChange} required={required} type={type} value={value} /></label>
  );
}
