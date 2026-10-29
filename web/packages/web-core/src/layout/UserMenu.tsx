import { Avatar, Menu, UnstyledButton } from '@mantine/core';
import { IconLogout, IconShieldLock } from '@tabler/icons-react';
import { Link } from 'react-router';
import { useAuth } from '../auth/useAuth';
import { useMe } from '../queries/me';

export function UserMenu() {
  const { logout } = useAuth();
  const me = useMe();
  const email = me.data?.email ?? '';

  return (
    <Menu position="bottom-end" width={240}>
      <Menu.Target>
        <UnstyledButton aria-label="Account">
          <Avatar color="teal" radius="xl" size="md">
            {email.slice(0, 2).toUpperCase()}
          </Avatar>
        </UnstyledButton>
      </Menu.Target>
      <Menu.Dropdown>
        <Menu.Label>{email}</Menu.Label>
        <Menu.Item
          component={Link}
          to="/account/security"
          leftSection={<IconShieldLock size={16} />}
        >
          Security
        </Menu.Item>
        <Menu.Item leftSection={<IconLogout size={16} />} onClick={() => void logout()}>
          Log out
        </Menu.Item>
      </Menu.Dropdown>
    </Menu>
  );
}
