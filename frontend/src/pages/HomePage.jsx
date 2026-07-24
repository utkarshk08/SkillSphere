import { Link } from 'react-router-dom';
import useAuth from '../hooks/useAuth';

export default function HomePage() {
  const { isAuthenticated } = useAuth();
  const destination = isAuthenticated ? '/dashboard' : '/register';

  return (
    <main className="home-page">
      <header className="home-header">
        <Link className="brand" to="/">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>SkillSphere</span>
        </Link>
        <div className="home-header-actions">
          {!isAuthenticated && <Link className="button button-secondary button-small" to="/login">Sign in</Link>}
          <Link className="button button-primary button-small" to={destination}>
            {isAuthenticated ? 'Open dashboard' : 'Create account'}
          </Link>
        </div>
      </header>

      <section className="home-hero">
        <p className="eyebrow">Student skill exchange platform</p>
        <h1>Teach skills. Learn skills. Build together.</h1>
        <p>
          SkillSphere helps students find peers through their skills, learning interests,
          communities, and mini-project collaborations.
        </p>
        <div className="home-hero-actions">
          <Link className="button button-primary" to={destination}>{isAuthenticated ? 'Go to dashboard' : 'Join SkillSphere'}</Link>
          {!isAuthenticated && <Link className="button button-secondary" to="/login">I already have an account</Link>}
        </div>
      </section>

      <section className="home-feature-grid" aria-label="SkillSphere features">
        <article className="home-feature-card">
          <h2>Share your skills</h2>
          <p>Add the skills you can teach and the skills you want to learn.</p>
        </article>
        <article className="home-feature-card">
          <h2>Find study partners</h2>
          <p>Search student profiles by name, college, country, skills, and interests.</p>
        </article>
        <article className="home-feature-card">
          <h2>Collaborate on projects</h2>
          <p>Create project opportunities and send collaboration requests to other students.</p>
        </article>
        <article className="home-feature-card">
          <h2>Learn in communities</h2>
          <p>Join communities, view their members and resources, and track your roadmaps.</p>
        </article>
      </section>
    </main>
  );
}

