/** Returns the concise backend error returned by the global exception handler. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  const data = error?.response?.data;

  if (typeof data === 'string') {
    return data;
  }

  if (data?.message) {
    return data.message;
  }

  if (data?.error) {
    return data.error;
  }

  if (data?.errors && typeof data.errors === 'object') {
    const firstError = Object.values(data.errors)[0];
    return Array.isArray(firstError) ? firstError[0] : firstError;
  }

  return fallback;
}

/** Spring Page responses always expose content; this also makes an empty fallback safe. */
export function asPage(data) {
  if (Array.isArray(data)) {
    return { content: data, totalPages: 1, totalElements: data.length, number: 0 };
  }

  return {
    content: data?.content || [],
    totalPages: data?.totalPages || 0,
    totalElements: data?.totalElements || 0,
    number: data?.number || 0,
  };
}

export function splitCommaSeparated(value) {
  if (Array.isArray(value)) {
    return value;
  }

  return String(value || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

export function joinValues(value) {
  return Array.isArray(value) ? value.join(', ') : value || '';
}

