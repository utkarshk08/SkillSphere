export default function Pagination({ page, pageNumber, onPageChange }) {
  const totalPages = page?.totalPages || 0;

  if (totalPages < 2) {
    return null;
  }

  return (
    <nav className="pagination" aria-label="Pagination">
      <button className="button button-secondary button-small" disabled={pageNumber === 0} onClick={() => onPageChange(pageNumber - 1)} type="button">
        Previous
      </button>
      <span>Page {pageNumber + 1} of {totalPages}</span>
      <button className="button button-secondary button-small" disabled={pageNumber >= totalPages - 1} onClick={() => onPageChange(pageNumber + 1)} type="button">
        Next
      </button>
    </nav>
  );
}

