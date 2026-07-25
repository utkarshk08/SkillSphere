import apiClient from './client';

/**
 * Keeps feature endpoint calls in one small file.  The React pages do not need to
 * know about Axios URLs, which makes the Spring Boot API contract easy to adjust
 * without spreading changes through the UI.
 */
function pageRequest(path, params = {}) {
  return apiClient.get(path, {
    params: {
      page: 0,
      size: 8,
      ...params,
    },
  });
}

function formDataForFile(file, fieldName = 'file') {
  const formData = new FormData();
  formData.append(fieldName, file);
  return formData;
}

function resourceApi(path) {
  return {
    list: (params) => pageRequest(path, params),
    get: (id) => apiClient.get(`${path}/${id}`),
    create: (data) => apiClient.post(path, data),
    update: (id, data) => apiClient.put(`${path}/${id}`, data),
    remove: (id) => apiClient.delete(`${path}/${id}`),
  };
}

export const profileApi = {
  list: ({ search, ...params } = {}) => pageRequest('/profiles', { ...params, name: search || undefined }),
  get: (username) => apiClient.get(`/profiles/${username}`),
  getMine: () => apiClient.get('/profiles/me'),
  updateMine: (data) => apiClient.put('/profiles/me', data),
  deleteMine: () => apiClient.delete('/profiles/me'),
  uploadPicture: (file) => apiClient.post('/profiles/me/picture', formDataForFile(file), {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
};

export const skillsApi = resourceApi('/skills');

export const projectsApi = {
  ...resourceApi('/projects'),
  mine: (params) => pageRequest('/projects/mine', params),
  uploadImage: (projectId, file) => apiClient.post(`/projects/${projectId}/images`, formDataForFile(file), {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  addMember: (projectId, userId) => apiClient.post(`/projects/${projectId}/members/${userId}`),
  removeMember: (projectId, userId) => apiClient.delete(`/projects/${projectId}/members/${userId}`),
};

export const communitiesApi = {
  ...resourceApi('/communities'),
  join: (id) => apiClient.post(`/communities/${id}/join`),
  leave: (id) => apiClient.delete(`/communities/${id}/leave`),
  members: (id, params) => pageRequest(`/communities/${id}/members`, params),
  projects: (id, params) => pageRequest(`/communities/${id}/projects`, params),
};

export const roadmapsApi = {
  ...resourceApi('/roadmaps'),
  mine: (params) => pageRequest('/roadmaps/mine', params),
};
export const bookmarksApi = resourceApi('/bookmarks');

export const collaborationRequestsApi = {
  ...resourceApi('/collaboration-requests'),
  updateStatus: (id, status, responseMessage = '') => apiClient.put(`/collaboration-requests/${id}`, {
    status,
    responseMessage: responseMessage.trim() || null,
  }),
  accept: (id, responseMessage = '') => apiClient.put(`/collaboration-requests/${id}`, {
    status: 'ACCEPTED',
    responseMessage: responseMessage.trim() || null,
  }),
  reject: (id, responseMessage = '') => apiClient.put(`/collaboration-requests/${id}`, {
    status: 'REJECTED',
    responseMessage: responseMessage.trim() || null,
  }),
};

export const notificationsApi = {
  ...resourceApi('/notifications'),
  unreadCount: () => apiClient.get('/notifications/unread-count'),
};

export const reportsApi = resourceApi('/reports');

export const announcementsApi = {
  list: (params) => pageRequest('/announcements', params),
};

export const adminApi = {
  users: (params) => pageRequest('/admin/users', params),
  deleteUser: (id) => apiClient.delete(`/admin/users/${id}`),
  verifyProfile: (id) => apiClient.put(`/admin/users/${id}/verify`),
  deleteContent: (contentType, contentId) => apiClient.delete(`/admin/content/${contentType}/${contentId}`),
  reports: (params) => pageRequest('/admin/reports', params),
  updateReport: (id, data) => apiClient.put(`/admin/reports/${id}`, data),
  deleteReport: (id) => apiClient.delete(`/admin/reports/${id}`),
  announcements: (params) => pageRequest('/admin/announcements', params),
  createAnnouncement: (data) => apiClient.post('/admin/announcements', data),
  updateAnnouncement: (id, data) => apiClient.put(`/admin/announcements/${id}`, data),
  deleteAnnouncement: (id) => apiClient.delete(`/admin/announcements/${id}`),
};
