import { Anchor, AppShell, Burger, Group, NavLink, Text } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { Link, Outlet, useLocation } from 'react-router';
import { ColorSchemeToggle } from './ColorSchemeToggle';
import { useMe } from '../queries/me';
import { UnitSwitcher } from '../units/UnitSwitcher';
import { navigation } from './navigation';
import { UserMenu } from './UserMenu';

/**
 * The frame around every logged-in page: header, menu, and the page itself in <main>. On a phone the menu folds
 * away behind the burger button.
 */
export function AppLayout() {
  const [menuOpen, menu] = useDisclosure(false);
  const { pathname } = useLocation();
  const me = useMe();
  const items = navigation.filter((item) => !item.role || me.data?.roles.includes(item.role));

  return (
    <AppShell
      header={{ height: 56 }}
      navbar={{ width: 240, breakpoint: 'sm', collapsed: { mobile: !menuOpen } }}
      padding="lg"
    >
      {/* The first thing a keyboard user reaches: jump past the menu straight to the page. */}
      <Anchor href="#main" className="skip-link">
        Skip to content
      </Anchor>

      <AppShell.Header>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              opened={menuOpen}
              onClick={menu.toggle}
              hiddenFrom="sm"
              size="sm"
              aria-label="Menu"
            />
            <Text component={Link} to="/" fw={700} size="lg" c="teal" td="none">
              Orudesk
            </Text>
          </Group>
          {/* nowrap: on a phone the header must stay one line, or it spills over the page */}
          <Group gap={6} wrap="nowrap">
            <UnitSwitcher />
            <ColorSchemeToggle />
            <UserMenu />
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="sm" aria-label="Main menu">
        {items.map((item) => (
          <NavLink
            key={item.path}
            component={Link}
            to={item.path}
            label={item.label}
            leftSection={item.icon}
            active={item.path === '/' ? pathname === '/' : pathname.startsWith(item.path)}
            onClick={menu.close}
          />
        ))}
      </AppShell.Navbar>

      <AppShell.Main id="main" tabIndex={-1}>
        <Outlet />
      </AppShell.Main>
    </AppShell>
  );
}
