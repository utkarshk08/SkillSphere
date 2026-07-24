export default function LoadingState({ message = 'Loading…' }) {
  return (
    <main className="page-center" aria-live="polite">
      <div className="loading-card">
        <span className="loading-spinner" aria-hidden="true" />
        <p>{message}</p>
      </div>
    </main>
  );
}
