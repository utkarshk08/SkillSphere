import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import AuthLayout from '../components/layout/AuthLayout';
import useAuth from '../hooks/useAuth';

const initialForm = {
  firstName: '',
  lastName: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  collegeName: '',
  course: '',
  yearOfStudy: '',
  country: '',
  bio: '',
};

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

function validate(form) {
  const errors = {};

  Object.entries(form).forEach(([field, value]) => {
    if (field !== 'bio' && !value.trim()) {
      errors[field] = 'This field is required.';
    }
  });

  if (form.email && !/^\S+@\S+\.\S+$/.test(form.email)) {
    errors.email = 'Enter a valid email address.';
  }

  if (form.password && !passwordPattern.test(form.password)) {
    errors.password = 'Use at least 8 characters with uppercase, lowercase, number, and special character.';
  }

  if (form.confirmPassword && form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match.';
  }

  return errors;
}

export default function RegisterPage() {
  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [submissionError, setSubmissionError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { isAuthenticated, register } = useAuth();
  const navigate = useNavigate();

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  function handleChange(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: undefined }));
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const nextErrors = validate(form);
    setErrors(nextErrors);
    setSubmissionError('');

    if (Object.keys(nextErrors).length > 0) {
      return;
    }

    setIsSubmitting(true);

    try {
      const result = await register(form);
      navigate(result.signedIn ? '/dashboard' : '/login', { replace: true });
    } catch (error) {
      setSubmissionError(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthLayout>
      <div className="auth-card auth-card-wide">
        <div className="auth-card-heading">
          <p className="eyebrow">Create your account</p>
          <h2>Join SkillSphere</h2>
          <p>Tell us the basics needed to set up your student account.</p>
        </div>

        {submissionError && (
          <p className="form-message form-message-error" role="alert">{submissionError}</p>
        )}

        <form className="auth-form" onSubmit={handleSubmit} noValidate>
          <div className="form-grid">
            <FormField error={errors.firstName} label="First name" name="firstName" onChange={handleChange} value={form.firstName} />
            <FormField error={errors.lastName} label="Last name" name="lastName" onChange={handleChange} value={form.lastName} />
            <FormField error={errors.username} label="Username" name="username" onChange={handleChange} value={form.username} />
            <FormField autoComplete="email" error={errors.email} label="Email" name="email" onChange={handleChange} type="email" value={form.email} />
            <FormField autoComplete="new-password" error={errors.password} label="Password" name="password" onChange={handleChange} type="password" value={form.password} />
            <FormField autoComplete="new-password" error={errors.confirmPassword} label="Confirm password" name="confirmPassword" onChange={handleChange} type="password" value={form.confirmPassword} />
            <FormField error={errors.collegeName} label="College name" name="collegeName" onChange={handleChange} value={form.collegeName} />
            <FormField error={errors.course} label="Course" name="course" onChange={handleChange} value={form.course} />
            <FormField error={errors.yearOfStudy} label="Year of study" name="yearOfStudy" onChange={handleChange} value={form.yearOfStudy} />
            <FormField error={errors.country} label="Country" name="country" onChange={handleChange} value={form.country} />
          </div>
          <label className="field">
            <span>Bio <em>(optional)</em></span>
            <textarea name="bio" onChange={handleChange} rows="3" value={form.bio} />
          </label>
          <p className="password-note">
            Password must have at least 8 characters, one uppercase letter, one lowercase letter,
            one number, and one special character.
          </p>
          <button className="button button-primary button-full" disabled={isSubmitting} type="submit">
            {isSubmitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </AuthLayout>
  );
}

function FormField({ autoComplete, error, label, name, onChange, type = 'text', value }) {
  return (
    <label className="field">
      <span>{label}</span>
      <input
        autoComplete={autoComplete}
        name={name}
        onChange={onChange}
        required
        type={type}
        value={value}
      />
      {error && <small className="field-error">{error}</small>}
    </label>
  );
}
