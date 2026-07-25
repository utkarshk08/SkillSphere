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

  async function changeStatus(request, status, responseMessage) {
    setActionError('');
    setNotice('');
    try {
      await collaborationRequestsApi.updateStatus(request.id, status, responseMessage);
      setNotice(
        status === 'ACCEPTED' && request.projectId
          ? 'Project application accepted. The student was added to the project team.'
          : `Request ${status === 'ACCEPTED' ? 'accepted' : 'rejected'}.`,
      );
      await reload({ page: pageNumber });
      return true;
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to update the collaboration request.'));
      return false;
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
      <PageHeader eyebrow="Collaboration requests" title="Requests between students" description="Review general requests and project applications. Accepting a project application adds that student to the project team." />
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
  const [responseMessage, setResponseMessage] = useState('');
  const [isUpdating, setIsUpdating] = useState(false);
  const received = Number(request.receiverId) === Number(currentUserId);
  const otherName = received ? (request.senderFullName || request.senderUsername) : (request.receiverFullName || request.receiverUsername);
  const otherUsername = received ? request.senderUsername : request.receiverUsername;
  const pending = request.status === 'PENDING';

  async function submitStatus(status) {
    setIsUpdating(true);
    await onStatusChange(request, status, responseMessage);
    setIsUpdating(false);
  }

  return (
    <article className="content-card request-card">
      <div className="card-heading-row">
        <div><p className="eyebrow">{received ? 'Received from' : 'Sent to'}</p><h2>{otherName}</h2><p className="muted-copy">@{otherUsername}</p></div>
        <span className={`status-pill status-${String(request.status).toLowerCase()}`}>{humanize(request.status)}</span>
      </div>
      {request.projectId ? (
        <div className="request-project-context">
          <span>Project application</span>
          <Link to="/projects">{request.projectTitle || `Project #${request.projectId}`}</Link>
        </div>
      ) : (
        <p className="muted-copy">General collaboration request</p>
      )}
      <p className="request-message">{request.message}</p>
      {!pending && request.responseMessage && (
        <div className="request-response">
          <p className="card-label">{received ? 'Your response' : 'Response received'}</p>
          <p className="request-message">{request.responseMessage}</p>
        </div>
      )}
      <p className="muted-copy">{formatDate(request.createdAt)}</p>
      {received && pending && (
        <div className="request-response-form">
          <label className="field">
            <span>Personalized response <em>(optional)</em></span>
            <textarea
              disabled={isUpdating}
              maxLength="1000"
              onChange={(event) => setResponseMessage(event.target.value)}
              placeholder="Add a short reply before accepting or rejecting."
              rows="3"
              value={responseMessage}
            />
            <small>{responseMessage.length}/1000 characters</small>
          </label>
          <div className="form-actions">
            <button className="button button-primary button-small" disabled={isUpdating} onClick={() => submitStatus('ACCEPTED')} type="button">{isUpdating ? 'Saving…' : request.projectId ? 'Accept and add member' : 'Accept'}</button>
            <button className="button button-secondary button-small" disabled={isUpdating} onClick={() => submitStatus('REJECTED')} type="button">Reject</button>
          </div>
        </div>
      )}
      <div className="card-actions card-actions-wrap">
        {otherUsername && <Link className="button button-secondary button-small" to={`/profiles/${otherUsername}`}>View profile</Link>}
        <button className="button button-danger button-small" onClick={() => onDelete(request)} type="button">Delete</button>
      </div>
    </article>
  );
}
