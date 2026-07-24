import { SERVER_BASE_URL } from '../api/client';

export function displayName(person) {
  const fullName = person?.fullName || [person?.firstName, person?.lastName].filter(Boolean).join(' ');
  return fullName || person?.username || person?.email || 'Student';
}

export function initials(person) {
  const name = displayName(person);
  return name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase();
}

export function mediaUrl(path) {
  if (!path) {
    return '';
  }

  return /^https?:\/\//i.test(path)
    ? path
    : `${SERVER_BASE_URL}/uploads/${path.replace(/^\/+/, '')}`;
}

export function formatDate(value) {
  if (!value) {
    return 'Not set';
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString();
}

export function humanize(value) {
  return String(value || '')
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}
