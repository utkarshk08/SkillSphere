import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { announcementsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import useAuth from '../hooks/useAuth';
import { displayName } from '../utils/display';

export default function DashboardPage() {
  const { user } = useAuth();
  const name = displayName(user);
  const [announcements, setAnnouncements] = useState([]);

  useEffect(() => {
    let active = true;
    announcementsApi.list({ page: 0, size: 3 })
      .then((response) => { if (active) setAnnouncements(response.data?.content || []); })
      .catch(() => { if (active) setAnnouncements([]); });
    return () => { active = false; };
  }, []);

  return (
    <AppShell>
      <PageHeader
        eyebrow="Dashboard"
        title={`Welcome, ${name}.`}
        description="Use your profile, skills, communities, and projects to connect with other students."
      />

      <section className="dashboard-grid">
        <article className="dashboard-card dashboard-card-profile">
          <p className="eyebrow">Your account</p>
          <h2>{name}</h2>
          <dl className="account-details">
            <div><dt>Username</dt><dd>{user?.username || 'Not available'}</dd></div>
            <div><dt>Email</dt><dd>{user?.email || 'Not available'}</dd></div>
            <div><dt>Profile</dt><dd>{user?.verified ? 'Verified' : 'Not verified'}</dd></div>
          </dl>
          <Link className="text-link" to="/profile">View and update profile</Link>
        </article>

        <article className="dashboard-card">
          <p className="eyebrow">Get started</p>
          <h2>Build your SkillSphere presence</h2>
          <div className="quick-link-list">
            <Link to="/skills">Add teaching or learning skills</Link>
            <Link to="/projects">Create or explore a project</Link>
            <Link to="/communities">Browse skill communities</Link>
            <Link to="/roadmaps">Create a learning roadmap</Link>
          </div>
        </article>

        <article className="dashboard-card dashboard-card-wide">
          <p className="eyebrow">Discover students</p>
          <h2>Find the right peer for your next learning goal.</h2>
          <p>Search profiles by name, college, country, skills, communities, projects, and learning interests.</p>
          <Link className="button button-primary" to="/search">Find students</Link>
        </article>
        {announcements.length > 0 && (
          <article className="dashboard-card dashboard-card-wide">
            <p className="eyebrow">Announcements</p>
            <h2>Latest from SkillSphere</h2>
            <div className="dashboard-announcements">
              {announcements.map((announcement) => <div key={announcement.id}><strong>{announcement.title}</strong><p>{announcement.message}</p></div>)}
            </div>
          </article>
        )}
      </section>
    </AppShell>
  );
}
