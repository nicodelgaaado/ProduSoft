'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { Header as CarbonHeader, HeaderGlobalAction, HeaderGlobalBar, HeaderMenuItem, HeaderNavigation, HeaderName, SkipToContent, Theme } from '@carbon/react';
import { Logout } from '@carbon/icons-react';
import { useAuth } from '@/hooks/useAuth';
import styles from './Header.module.css';

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
    <Theme theme="g100">
      <CarbonHeader aria-label="ProduSoft console">
        <SkipToContent />
        <HeaderName href="/" prefix="ProduSoft">
          Workflow
        </HeaderName>
        {visibleLinks.length > 0 && (
          <HeaderNavigation aria-label="ProduSoft navigation">
            {visibleLinks.map((link) => (
              <HeaderMenuItem
                key={link.href}
                as={Link}
                href={link.href}
                isCurrentPage={pathname === link.href}
              >
                {link.label}
              </HeaderMenuItem>
            ))}
          </HeaderNavigation>
        )}
        <HeaderGlobalBar>
          {user && (
            <>
              <span className={styles.username}>{user.username}</span>
              <HeaderGlobalAction
                aria-label="Log out"
                tooltipAlignment="end"
                onClick={handleLogout}
              >
                <Logout size={20} />
              </HeaderGlobalAction>
            </>
          )}
        </HeaderGlobalBar>
      </CarbonHeader>
    </Theme>
  );
}

