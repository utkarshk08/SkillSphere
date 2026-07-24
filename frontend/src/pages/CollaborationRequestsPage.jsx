import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { collaborationRequestsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import useAuth from '../hooks/useAuth';
import usePagedResource from '../hooks/usePagedResource';
import { formatDate, humanize } from '../utils/display';
import { getErrorMessage } from '../utils/http';

export default function CollaborationRequestsPage() {
  const { user } = useAuth();
  const loadRequests = useCallback((params) => collaborationRequestsApi.list(params), []);
  const { page, pageNumber, setPageNumber, setFilters, isLoading, error, reload } = usePagedResource(loadRequests, { search: '' });
  const [search, setSearch] = useState('');
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');

  function submitSearch(event) {
    event.preventDefault();
    setFilters({ search: search.trim() });
  }

  async function changeStatus(request, status) {
    setActionError('');
    setNotice('');
    try {
      await collaborationRequestsApi.updateStatus(request.id, status);
      setNotice(`Request ${status === 'ACCEPTED' ? 'accepted' : 'rejected'}.`);
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to update the collaboration request.'));
    }
  }

  async function deleteRequest(request) {
    if (!window.confirm('Delete this collaboration request?')) return;
    setActionError('');
    try {
      await collaborationRequestsApi.remove(request.id);
      setNotice('Collaboration request deleted.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to delete the collaboration request.'));
    }
  }

  return (
    <AppShell>
      <PageHeader eyebrow="Collaboration requests" title="Requests between students" description="Review collaboration requests you sent or received. Send a new request from a student profile." />
      <form className="list-toolbar" onSubmit={submitSearch}>
        <label className="search-field"><span className="sr-only">Search requests</span><input onChange={(event) => setSearch(event.target.value)} placeholder="Search message or student" value={search} /></label>
        <button className="button button-secondary" type="submit">Search</button>
      </form>
      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {notice && <p className="form-message form-message-success" role="status">{notice}</p>}
      {isLoading ? <InlineLoading message="Loading collaboration requests…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No collaboration requests" message="Search students and send a request from their profile when you are ready to collaborate." action={<Link className="button button-primary" to="/search">Find students</Link>} />
      ) : (
        <>
          <section className="request-list">
            {page.content.map((request) => <RequestCard currentUserId={user?.id} key={request.id} onDelete={deleteRequest} onStatusChange={changeStatus} request={request} />)}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function RequestCard({ request, currentUserId, onStatusChange, onDelete }) {
  const received = Number(request.receiverId) === Number(currentUserId);
  const otherName = received ? (request.senderFullName || request.senderUsername) : (request.receiverFullName || request.receiverUsername);
  const otherUsername = received ? request.senderUsername : request.receiverUsername;
  const pending = request.status === 'PENDING';

  return (
    <article className="content-card request-card">
      <div className="card-heading-row">
        <div><p className="eyebrow">{received ? 'Received from' : 'Sent to'}</p><h2>{otherName}</h2><p className="muted-copy">@{otherUsername}</p></div>
        <span className={`status-pill status-${String(request.status).toLowerCase()}`}>{humanize(request.status)}</span>
      </div>
      <p className="request-message">{request.message}</p>
      <p className="muted-copy">{formatDate(request.createdAt)}</p>
      <div className="card-actions card-actions-wrap">
        {otherUsername && <Link className="button button-secondary button-small" to={`/profiles/${otherUsername}`}>View profile</Link>}
        {received && pending && <><button className="button button-primary button-small" onClick={() => onStatusChange(request, 'ACCEPTED')} type="button">Accept</button><button className="button button-secondary button-small" onClick={() => onStatusChange(request, 'REJECTED')} type="button">Reject</button></>}
        <button className="button button-danger button-small" onClick={() => onDelete(request)} type="button">Delete</button>
      </div>
    </article>
  );
}

