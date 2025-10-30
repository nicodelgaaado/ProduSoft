'use client';

import { Button, InlineNotification, Stack, TextInput, Tile } from '@carbon/react';
import { FormEvent, Suspense, useEffect, useMemo, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import styles from './login.module.css';

export default function LoginPage() {
  return (
    <Suspense fallback={<div />}>
      <LoginPageContent />
    </Suspense>
  );
}

function LoginPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { user, sessions, login, loading, error, clearError } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const addingAccount = searchParams.get('addAccount') === '1';

  const defaultDestination = useMemo(() => {
    if (!user) {
      return null;
    }
    return user.roles.includes('SUPERVISOR') ? '/supervisor' : '/operator';
  }, [user]);

  useEffect(() => {
    if (addingAccount) {
      return;
    }
    if (!defaultDestination) {
      return;
    }
    router.replace(defaultDestination);
  }, [defaultDestination, router, addingAccount]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      const profile = await login(username.trim(), password);
      const destination = profile.roles.includes('SUPERVISOR') ? '/supervisor' : '/operator';
      router.replace(destination);
    } catch (err) {
      console.error('Login failed', err);
    } finally {
      setSubmitting(false);
    }
  };

  useEffect(() => {
    clearError();
  }, [username, password, clearError]);

  const busy = submitting || loading;

  return (
    <div className={styles.container}>
      <Tile className={styles.card}>
        <Stack gap={6}>
          <div>
            <h2 className="cds--heading-05">Sign in</h2>
            <p className={styles.subtitle}>Use your operator or supervisor account to continue.</p>
            {sessions.length > 1 && !addingAccount && (
              <p className={styles.subtitle}>
                You have {sessions.length} accounts available. Use the profile menu to switch between them.
              </p>
            )}
          </div>

          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              title="Sign-in failed"
              subtitle={error}
              onClose={clearError}
            />
          )}

          <form className={styles.form} onSubmit={handleSubmit}>
            <Stack gap={4}>
              <TextInput
                id="username"
                name="username"
                labelText="Username"
                autoComplete="username"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                placeholder="operator1"
                required
              />
              <TextInput
                id="password"
                name="password"
                labelText="Password"
                autoComplete="current-password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="••••••"
                required
              />
              <Button type="submit" kind="primary" size="lg" disabled={busy}>
                {busy ? 'Signing in…' : 'Sign in'}
              </Button>
            </Stack>
          </form>

          <div className={styles.hint}>
            <p>Sample accounts:</p>
            <ul>
              <li>
                Operator — <code>operator1 / user</code>
              </li>
              <li>
                Supervisor — <code>supervisor1 / superuser</code>
              </li>
            </ul>
          </div>
        </Stack>
      </Tile>
    </div>
  );
}
