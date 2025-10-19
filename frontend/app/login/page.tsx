'use client';

import { Button, Heading, InlineNotification, Stack, TextInput, Tile } from '@carbon/react';
import { FormEvent, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';
import styles from './login.module.css';

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

  const busy = submitting || loading;

  return (
    <div className={styles.container}>
      <Tile className={styles.card}>
        <Stack gap={6}>
          <div>
            <Heading level={2}>Sign in</Heading>
            <p className={styles.subtitle}>Use your operator or supervisor account to continue.</p>
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
