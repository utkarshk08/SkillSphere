import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { reportsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import usePagedResource from '../hooks/usePagedResource';
import { formatDate, humanize } from '../utils/display';
import { getErrorMessage } from '../utils/http';

const emptyReport = {
  reportedUserId: '',
  reportedContentType: '',
  reportedContentId: '',
  reason: 'SPAM',
  description: '',
};

export default function ReportsPage() {
  const [searchParams] = useSearchParams();
  const loadReports = useCallback((params) => reportsApi.list(params), []);
  const { page, pageNumber, setPageNumber, isLoading, error, reload } = usePagedResource(loadReports);
  const [form, setForm] = useState(() => ({ ...emptyReport, reportedUserId: searchParams.get('reportedUserId') || '' }));
  const [editingReport, setEditingReport] = useState(null);
  const [showForm, setShowForm] = useState(Boolean(searchParams.get('reportedUserId')));
  const [actionError, setActionError] = useState('');
  const [notice, setNotice] = useState('');
  const [isSaving, setIsSaving] = useState(false);

  useEffect(() => {
    const reportedUserId = searchParams.get('reportedUserId');
    if (reportedUserId) {
      setForm((current) => ({ ...current, reportedUserId }));
      setShowForm(true);
    }
  }, [searchParams]);

  function openCreate() {
    setForm(emptyReport);
    setEditingReport(null);
    setActionError('');
    setShowForm(true);
  }

  function openEdit(report) {
    setForm({
      ...emptyReport,
      reportedUserId: report.reportedUserId || '',
      reportedContentType: report.reportedContentType || '',
      reportedContentId: report.reportedContentId || '',
      reason: report.reason || 'SPAM',
      description: report.description || '',
    });
    setEditingReport(report);
    setActionError('');
    setShowForm(true);
  }

  async function saveReport(event) {
    event.preventDefault();
    setIsSaving(true);
    setActionError('');
    try {
      if (editingReport) {
        await reportsApi.update(editingReport.id, { reason: form.reason, description: form.description || null });
      } else {
        await reportsApi.create({
          reportedUserId: form.reportedUserId ? Number(form.reportedUserId) : null,
          reportedContentType: form.reportedContentType || null,
          reportedContentId: form.reportedContentId ? Number(form.reportedContentId) : null,
          reason: form.reason,
          description: form.description || null,
        });
      }
      setShowForm(false);
      setNotice(editingReport ? 'Report updated.' : 'Report submitted for review.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to save the report.'));
    } finally {
      setIsSaving(false);
    }
  }

  async function deleteReport(report) {
    if (!window.confirm('Delete this report?')) return;
    setActionError('');
    try {
      await reportsApi.remove(report.id);
      setNotice('Report deleted.');
      reload({ page: pageNumber });
    } catch (requestError) {
      setActionError(getErrorMessage(requestError, 'Unable to delete the report.'));
    }
  }

  return (
    <AppShell>
      <PageHeader eyebrow="Reports" title="Reports you submitted" description="Report spam, fake profiles, or abusive content. An administrator reviews the report and records its resolution." actions={<button className="button button-primary" onClick={openCreate} type="button">Create report</button>} />
      {actionError && <p className="form-message form-message-error" role="alert">{actionError}</p>}
      {notice && <p className="form-message form-message-success" role="status">{notice}</p>}
      {showForm && <ReportForm editing={Boolean(editingReport)} form={form} isSaving={isSaving} onCancel={() => setShowForm(false)} onChange={(event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }))} onSubmit={saveReport} />}
      {isLoading ? <InlineLoading message="Loading reports…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No reports submitted" message="Use a report only for spam, fake profiles, or abusive content." />
      ) : (
        <>
          <section className="report-list">
            {page.content.map((report) => <ReportCard key={report.id} onDelete={deleteReport} onEdit={openEdit} report={report} />)}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function ReportCard({ report, onEdit, onDelete }) {
  return (
    <article className="content-card report-card">
      <div className="card-heading-row"><div><span className="status-pill status-muted">{humanize(report.reason)}</span><h2>{report.reportedUsername || (report.reportedContentType ? `${humanize(report.reportedContentType)} report` : 'Report')}</h2><p className="muted-copy">Submitted {formatDate(report.createdAt)}</p></div><span className={`status-pill status-${String(report.status).toLowerCase()}`}>{humanize(report.status)}</span></div>
      {report.description && <p>{report.description}</p>}
      {report.adminAction && report.adminAction !== 'NONE' && <p className="muted-copy">Admin action: {humanize(report.adminAction)}</p>}
      <div className="card-actions"><button className="button button-secondary button-small" onClick={() => onEdit(report)} type="button">Edit</button><button className="button button-danger button-small" onClick={() => onDelete(report)} type="button">Delete</button></div>
    </article>
  );
}

function ReportForm({ form, onChange, onSubmit, onCancel, isSaving, editing }) {
  const hasUserTarget = Boolean(form.reportedUserId);
  const hasContentTarget = Boolean(form.reportedContentType || form.reportedContentId);

  return (
    <form className="content-card form-card" onSubmit={onSubmit}>
      <div className="form-section-heading"><h2>{editing ? 'Edit report' : 'Create report'}</h2><p>Select a reason and identify the user or content when available.</p></div>
      {!editing && <div className="form-grid">
        <label className="field"><span>Reported user ID</span><input disabled={hasContentTarget} min="1" name="reportedUserId" onChange={onChange} type="number" value={form.reportedUserId} /></label>
        <label className="field"><span>Reported content type</span><select disabled={hasUserTarget} name="reportedContentType" onChange={onChange} value={form.reportedContentType}><option value="">No content target</option><option value="PROFILE">Profile</option><option value="PROJECT">Project</option><option value="COMMUNITY">Community</option><option value="SKILL">Skill</option></select></label>
        <label className="field"><span>Reported content ID</span><input disabled={hasUserTarget || !form.reportedContentType} min="1" name="reportedContentId" onChange={onChange} type="number" value={form.reportedContentId} /></label>
      </div>}
      {!editing && <p className="muted-copy">Choose exactly one target: a user or a content item.</p>}
      <label className="field"><span>Reason</span><select name="reason" onChange={onChange} value={form.reason}><option value="SPAM">Spam</option><option value="FAKE_PROFILE">Fake profile</option><option value="ABUSIVE_CONTENT">Abusive content</option></select></label>
      <label className="field"><span>Description (optional)</span><textarea maxLength="1500" name="description" onChange={onChange} rows="4" value={form.description} /></label>
      <div className="form-actions"><button className="button button-primary" disabled={isSaving} type="submit">{isSaving ? 'Saving…' : editing ? 'Save report' : 'Submit report'}</button><button className="button button-secondary" onClick={onCancel} type="button">Cancel</button></div>
    </form>
  );
}
