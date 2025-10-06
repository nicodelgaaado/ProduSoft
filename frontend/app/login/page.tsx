'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

export default function LoginPage() {
  const router = useRouter();
  const { user, login, loading, error, clearError } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!user) return;
    if (user.roles.includes('SUPERVISOR')) {
      router.replace('/supervisor');
    } else if (user.roles.includes('OPERATOR')) {
      router.replace('/operator');
    }
  }, [user, router]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await login(username.trim(), password);
    } catch (err) {
      console.error('Login failed', err);
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    clearError();
  }, [username, password, clearError]);

  return (
    <section className="auth-card">
      <h1>Sign in</h1>
      <p className="auth-card__subtitle">Use your operator or supervisor account to continue.</p>
      <form className="auth-card__form" onSubmit={handleSubmit}>
        <label htmlFor="username">Username</label>
        <input
          id="username"
          name="username"
          type="text"
          autoComplete="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="operator1"
          required
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="••••••"
          required
        />

        {error && <div className="form-error">{error}</div>}

        <button type="submit" disabled={submitting || loading}>
          {submitting || loading ? 'Signing in...' : 'Sign in'}
        </button>
      </form>
      <div className="auth-card__hint">
        <p>Sample accounts:</p>
        <ul>
          <li>
            Operator — <code>operator1 / operator123</code>
          </li>
          <li>
            Supervisor — <code>supervisor1 / supervisor123</code>
          </li>
        </ul>
      </div>
    </section>
  );
}

