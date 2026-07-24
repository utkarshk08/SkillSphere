import LoadingState from './LoadingState';

export function InlineLoading({ message = 'Loading…' }) {
  return <LoadingState message={message} />;
}

export function ErrorState({ message, onRetry }) {
  return (
    <section className="state-card" role="alert">
      <h2>We could not load this page</h2>
      <p>{message}</p>
      {onRetry && <button className="button button-secondary" onClick={onRetry} type="button">Try again</button>}
    </section>
  );
}

export function EmptyState({ title = 'Nothing here yet', message, action }) {
  return (
    <section className="state-card">
      <h2>{title}</h2>
      {message && <p>{message}</p>}
      {action}
    </section>
  );
}

