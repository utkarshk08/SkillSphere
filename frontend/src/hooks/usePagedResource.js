import { useCallback, useEffect, useState } from 'react';
import { asPage, getErrorMessage } from '../utils/http';

/**
 * Shared pagination state for the lists required by the project.  Each page only
 * supplies its endpoint call and filters; Spring Page details stay in one place.
 */
export default function usePagedResource(loadPage, initialFilters = {}, pageSize = 8, enabled = true) {
  const [filters, setFilters] = useState(initialFilters);
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState(() => asPage());
  const [isLoading, setIsLoading] = useState(enabled);
  const [error, setError] = useState('');

  const load = useCallback(async (overrides = {}) => {
    if (!enabled) {
      setPage(asPage());
      setError('');
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setError('');

    try {
      const response = await loadPage({
        ...filters,
        ...overrides,
        page: overrides.page ?? pageNumber,
        size: pageSize,
      });
      setPage(asPage(response.data));
    } catch (requestError) {
      setError(getErrorMessage(requestError, 'Unable to load this information.'));
    } finally {
      setIsLoading(false);
    }
  }, [enabled, filters, loadPage, pageNumber, pageSize]);

  useEffect(() => {
    load();
  }, [load]);

  const updateFilters = useCallback((nextFilters) => {
    setPageNumber(0);
    setFilters((current) => ({ ...current, ...nextFilters }));
  }, []);

  return {
    filters,
    setFilters: updateFilters,
    page,
    pageNumber,
    setPageNumber,
    isLoading,
    error,
    reload: load,
  };
}
