'use client';

import { useEffect, useMemo } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

type RequireAuthProps = {
  allowedRoles?: string[];
  children: React.ReactNode;
};

export function RequireAuth({ allowedRoles, children }: RequireAuthProps) {
  const router = useRouter();
  const { user, loading } = useAuth();

  const isAuthorised = useMemo(() => {
    if (!user) return false;
    if (!allowedRoles || allowedRoles.length === 0) return true;
    return allowedRoles.some((role) => user.roles.includes(role));
  }, [user, allowedRoles]);

  useEffect(() => {
    if (loading) return;
    if (!user) {
      router.replace('/login');
      return;
    }
    if (!isAuthorised) {
      router.replace('/');
    }
  }, [user, loading, isAuthorised, router]);

  if (loading) {
    return (
      <div className="page-status" role="status">
        Checking access...
      </div>
    );
  }

  if (!user || !isAuthorised) {
    return null;
  }

  return <>{children}</>;
}

