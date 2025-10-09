'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

const navLinks = [
  { href: '/operator', label: 'Operator Console', roles: ['OPERATOR'] },
  { href: '/supervisor', label: 'Supervisor Dashboard', roles: ['SUPERVISOR'] },
];

export function Header() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, logout } = useAuth();

  const visibleLinks = navLinks.filter((link) =>
    user?.roles.some((role) => link.roles.includes(role)),
  );

  const handleLogout = () => {
    logout();
    router.replace('/login');
  };

  return (
    <header className="app-header">
      <div className="app-header__brand">ProduSoft</div>
      <nav className="app-header__nav">
        {visibleLinks.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className={pathname === link.href ? 'active' : ''}
          >
            {link.label}
          </Link>
        ))}
      </nav>
      {user && (
        <div className="app-header__user">
          <span>{user.username}</span>
          <button type="button" onClick={handleLogout} className="link-button">
            Log out
          </button>
        </div>
      )}
    </header>
  );
}

