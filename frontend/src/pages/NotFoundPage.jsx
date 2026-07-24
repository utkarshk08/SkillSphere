import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <main className="page-center">
      <section className="not-found-card">
        <p className="eyebrow">404</p>
        <h1>Page not found</h1>
        <p>The page you requested is not available.</p>
        <Link className="button button-primary" to="/dashboard">Go to dashboard</Link>
      </section>
    </main>
  );
}
