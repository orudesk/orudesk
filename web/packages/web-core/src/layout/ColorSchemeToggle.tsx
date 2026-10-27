import { ActionIcon, useComputedColorScheme, useMantineColorScheme } from '@mantine/core';
import { IconMoon, IconSun } from '@tabler/icons-react';

/** Light or dark. Mantine remembers the choice in localStorage; until then it follows the system setting. */
export function ColorSchemeToggle() {
  const { setColorScheme } = useMantineColorScheme();
  const computed = useComputedColorScheme('light');
  const next = computed === 'dark' ? 'light' : 'dark';
  return (
    <ActionIcon
      variant="subtle"
      color="gray"
      size="lg"
      aria-label={`Switch to ${next} theme`}
      onClick={() => setColorScheme(next)}
    >
      {computed === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
    </ActionIcon>
  );
}
