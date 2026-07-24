import { Link } from 'react-router-dom';

export default function AuthLayout({ children }) {
  return (
    <main className="auth-page">
      <section className="auth-intro" aria-label="About SkillSphere">
        <Link className="brand brand-light" to="/login">
          <span className="brand-mark" aria-hidden="true">S</span>
          <span>SkillSphere</span>
        </Link>
        <div className="auth-intro-copy">
          <p className="eyebrow eyebrow-light">Student skill exchange</p>
          <h1>Learn together. Build together.</h1>
          <p>
            A focused space for students to connect through the skills they teach,
            the skills they are learning, and the projects they build.
          </p>
        </div>
      </section>
      <section className="auth-content">{children}</section>
    </main>
  );
}
