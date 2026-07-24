import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import { bookmarksApi, collaborationRequestsApi, profileApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import { ErrorState, InlineLoading } from '../components/shared/ResourceState';
import useAuth from '../hooks/useAuth';
import { displayName, initials, mediaUrl } from '../utils/display';
import { getErrorMessage } from '../utils/http';

function valueList(value) {
  if (Array.isArray(value)) {
    return value;
  }

  return typeof value === 'string' ? value.split(',').map((item) => item.trim()).filter(Boolean) : [];
}

function TagSection({ title, values, emptyMessage }) {
  return (
    <section className="content-card">
      <h2>{title}</h2>
      {values.length ? <div className="tag-list">{values.map((value) => <span className="tag" key={value}>{value}</span>)}</div> : <p className="muted-copy">{emptyMessage}</p>}
    </section>
  );
}

export default function StudentProfilePage() {
  const { username } = useParams();
  const location = useLocation();
  const { isAuthenticated } = useAuth();
  const [profile, setProfile] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [requestMessage, setRequestMessage] = useState('');
  const [isSending, setIsSending] = useState(false);

  async function loadProfile() {
    setIsLoading(true);
    setError('');
    try {
      const response = await profileApi.get(username);
      setProfile(response.data);
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to load this profile.'));
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadProfile();
  }, [username]);

  async function bookmarkProfile() {
    setError('');
    try {
      await bookmarksApi.create({ targetType: 'PROFILE', targetId: profile.id });
      setMessage('Profile bookmarked.');
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to bookmark this profile.'));
    }
  }

  async function sendRequest(event) {
    event.preventDefault();
    setIsSending(true);
    setError('');
    try {
      await collaborationRequestsApi.create({ receiverId: profile.id, message: requestMessage });
      setRequestMessage('');
      setMessage('Collaboration request sent.');
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to send the collaboration request.'));
    } finally {
      setIsSending(false);
    }
  }

  if (isLoading) {
    return <InlineLoading message="Loading student profile…" />;
  }

  if (!profile) {
    return <AppShell publicView={!isAuthenticated}><ErrorState message={error || 'This profile is not available.'} onRetry={loadProfile} /></AppShell>;
  }

  const name = displayName(profile);
  const photoUrl = mediaUrl(profile.profilePicturePath);
  const interests = valueList(profile.interests);
  const teachingSkills = valueList(profile.teachingSkills || profile.canTeachSkills);
  const learningSkills = valueList(profile.currentLearningSkills || profile.learningSkills || profile.wantToLearnSkills);
  const links = [
    ['GitHub', profile.githubUrl],
    ['LinkedIn', profile.linkedinUrl],
    ['Portfolio', profile.portfolioUrl],
  ].filter(([, url]) => url);

  return (
    <AppShell publicView={!isAuthenticated}>
      <PageHeader
        eyebrow="Student profile"
        title={name}
        description={`@${profile.username || 'student'}`}
        actions={<Link className="button button-secondary" to={isAuthenticated ? '/search' : '/'}>{isAuthenticated ? 'Back to search' : 'Back to home'}</Link>}
      />
      {error && <p className="form-message form-message-error" role="alert">{error}</p>}
      {message && <p className="form-message form-message-success" role="status">{message}</p>}

      <section className="student-profile-layout">
        <aside className="profile-summary-card">
          <div className="profile-avatar profile-avatar-large">
            {photoUrl ? <img alt={`${name}'s profile`} src={photoUrl} /> : <span>{initials(profile)}</span>}
          </div>
          <h2>{name}</h2>
          <p>{profile.collegeName || profile.college || 'College not added'}</p>
          {profile.verified && <span className="status-pill status-verified">Verified profile</span>}
          {isAuthenticated ? (
            <button className="button button-secondary button-full" onClick={bookmarkProfile} type="button">Bookmark profile</button>
          ) : (
            <Link className="button button-secondary button-full" state={{ from: location }} to="/login">Sign in to bookmark</Link>
          )}
        </aside>

        <div className="profile-content">
          <section className="content-card profile-details-card">
            <h2>About</h2>
            <p>{profile.bio || 'No bio added yet.'}</p>
            <dl className="detail-list">
              <div><dt>College</dt><dd>{profile.collegeName || profile.college || 'Not added'}</dd></div>
              <div><dt>Course</dt><dd>{profile.course || 'Not added'}</dd></div>
              <div><dt>Year</dt><dd>{profile.yearOfStudy || profile.year || 'Not added'}</dd></div>
              <div><dt>Country</dt><dd>{profile.country || 'Not added'}</dd></div>
            </dl>
            {links.length > 0 && (
              <div className="external-links">
                {links.map(([label, url]) => <a href={url} key={label} rel="noreferrer" target="_blank">{label}</a>)}
              </div>
            )}
          </section>

          <TagSection emptyMessage="No teaching skills added." title="Skills they can teach" values={teachingSkills} />
          <TagSection emptyMessage="No current learning skills added." title="Current learning skills" values={learningSkills} />
          <TagSection emptyMessage="No interests added." title="Interests" values={interests} />

          {isAuthenticated ? (
            <>
              <form className="content-card compact-form" onSubmit={sendRequest}>
                <h2>Send collaboration request</h2>
                <p>Introduce your collaboration need with a short message.</p>
                <label className="field"><span>Message</span><textarea onChange={(event) => setRequestMessage(event.target.value)} required rows="3" value={requestMessage} /></label>
                <button className="button button-primary" disabled={isSending} type="submit">{isSending ? 'Sending…' : 'Send request'}</button>
              </form>
              <Link className="button button-text" to={`/reports?reportedUserId=${profile.id}`}>Report this profile</Link>
            </>
          ) : (
            <Link className="button button-primary" state={{ from: location }} to="/login">Sign in to collaborate or report</Link>
          )}
        </div>
      </section>
    </AppShell>
  );
}
