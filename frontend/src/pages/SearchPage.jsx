import { useCallback, useState } from 'react';
import { Link } from 'react-router-dom';
import { communitiesApi, profileApi, projectsApi } from '../api/platformApi';
import AppShell from '../components/layout/AppShell';
import PageHeader from '../components/shared/PageHeader';
import Pagination from '../components/shared/Pagination';
import { EmptyState, ErrorState, InlineLoading } from '../components/shared/ResourceState';
import usePagedResource from '../hooks/usePagedResource';
import { displayName, humanize } from '../utils/display';

const initialSearch = { search: '', college: '', country: '', skill: '', interest: '' };

export default function SearchPage() {
  const [searchType, setSearchType] = useState('profiles');
  const [form, setForm] = useState(initialSearch);
  const loadResults = useCallback((params) => {
    if (searchType === 'communities') return communitiesApi.list({ search: params.search, page: params.page, size: params.size });
    if (searchType === 'projects') return projectsApi.list({ search: params.search, page: params.page, size: params.size });
    return profileApi.list(params);
  }, [searchType]);
  const { page, pageNumber, setPageNumber, setFilters, isLoading, error, reload } = usePagedResource(loadResults, initialSearch);

  function submitSearch(event) {
    event.preventDefault();
    setFilters(Object.fromEntries(Object.entries(form).map(([key, value]) => [key, value.trim()])));
  }

  function changeType(event) {
    const type = event.target.value;
    setSearchType(type);
    setPageNumber(0);
    setFilters({ ...form });
  }

  return (
    <AppShell>
      <PageHeader eyebrow="Search" title="Find students, communities, and projects" description="Search student profiles by name, college, country, skills, and learning interests. You can also search community and project listings." />
      <form className="content-card search-form" onSubmit={submitSearch}>
        <div className="form-grid">
          <label className="field"><span>Search</span><input name="search" onChange={(event) => setForm((current) => ({ ...current, search: event.target.value }))} placeholder="Name, community, or project" value={form.search} /></label>
          <label className="field"><span>Search in</span><select onChange={changeType} value={searchType}><option value="profiles">Student profiles</option><option value="communities">Communities</option><option value="projects">Projects</option></select></label>
          {searchType === 'profiles' && <>
            <label className="field"><span>College</span><input name="college" onChange={(event) => setForm((current) => ({ ...current, college: event.target.value }))} value={form.college} /></label>
            <label className="field"><span>Country</span><input name="country" onChange={(event) => setForm((current) => ({ ...current, country: event.target.value }))} value={form.country} /></label>
            <label className="field"><span>Skill</span><input name="skill" onChange={(event) => setForm((current) => ({ ...current, skill: event.target.value }))} value={form.skill} /></label>
            <label className="field"><span>Learning interest</span><input name="interest" onChange={(event) => setForm((current) => ({ ...current, interest: event.target.value }))} value={form.interest} /></label>
          </>}
        </div>
        <div className="form-actions"><button className="button button-primary" type="submit">Search</button></div>
      </form>

      {isLoading ? <InlineLoading message="Searching SkillSphere…" /> : error ? <ErrorState message={error} onRetry={reload} /> : page.content.length === 0 ? (
        <EmptyState title="No results found" message="Try changing your search terms or filters." />
      ) : (
        <>
          <p className="results-summary">{page.totalElements} result{page.totalElements === 1 ? '' : 's'} found</p>
          <section className="search-results">
            {page.content.map((result) => <SearchResult key={result.id} result={result} type={searchType} />)}
          </section>
          <Pagination page={page} pageNumber={pageNumber} onPageChange={setPageNumber} />
        </>
      )}
    </AppShell>
  );
}

function SearchResult({ result, type }) {
  if (type === 'communities') {
    return <article className="content-card search-result"><span className="status-pill status-muted">Community</span><h2>{result.name}</h2><p>{result.description}</p><div className="tag-list"><span className="tag">{result.memberCount ?? 0} members</span><span className="tag">{result.projectCount ?? 0} projects</span></div><Link className="text-link" to="/communities">View community</Link></article>;
  }
  if (type === 'projects') {
    return <article className="content-card search-result"><span className="status-pill status-muted">Project</span><h2>{result.title}</h2><p>{result.description}</p><div className="tag-list">{(result.requiredSkills || []).map((skill) => <span className="tag" key={skill}>{skill}</span>)}</div><p className="muted-copy">Status: {humanize(result.status)}</p><Link className="text-link" to="/projects">View project</Link></article>;
  }
  return (
    <article className="content-card search-result profile-search-result">
      <div><span className="status-pill status-muted">Student</span><h2>{displayName(result)}</h2><p className="muted-copy">@{result.username}</p><p>{result.collegeName || result.college || 'College not added'} · {result.country || 'Country not added'}</p></div>
      <div className="search-result-actions"><Link className="button button-secondary button-small" to={`/profiles/${result.username}`}>View profile</Link></div>
    </article>
  );
}
