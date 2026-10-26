import { Select, Text } from '@mantine/core';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo } from 'react';
import { useMe } from '../queries/me';
import { useOrgTree } from '../queries/orgUnits';
import { accessibleUnits } from './accessibleUnits';
import { useCurrentUnit } from './useCurrentUnit';

/**
 * Picks the org unit the user works in. Everything that happens "at a unit" (admitting a student at School A)
 * is sent to the API with this unit in the X-Org-Unit header.
 */
export function UnitSwitcher() {
  const me = useMe();
  const tree = useOrgTree();
  const { unitId, setUnitId } = useCurrentUnit();
  const queryClient = useQueryClient();

  const units = useMemo(
    () => (tree.data && me.data ? accessibleUnits(tree.data, me.data.grants) : []),
    [tree.data, me.data],
  );

  // No choice yet, or the remembered unit isn't this user's: start at the first one they may use.
  useEffect(() => {
    if (units.length > 0 && !units.some((unit) => unit.id === unitId)) {
      setUnitId(units[0]!.id);
    }
  }, [units, unitId, setUnitId]);

  const depthOf = new Map(units.map((unit) => [unit.id, unit.depth]));

  if (units.length === 0) return null;

  return (
    <Select
      aria-label="Org unit"
      data={units.map((unit) => ({ value: unit.id, label: unit.name }))}
      // Indent each option by its depth in the tree, so a school shows under its group
      renderOption={({ option }) => (
        <Text component="span" size="sm" pl={(depthOf.get(option.value) ?? 0) * 16}>
          {option.label}
        </Text>
      )}
      value={unitId}
      onChange={(value) => {
        if (!value || value === unitId) return;
        setUnitId(value);
        // What you may see and do depends on the unit: ask the API again for everything on screen.
        void queryClient.invalidateQueries();
      }}
      allowDeselect={false}
      searchable={units.length > 7}
      w={{ base: 136, sm: 240 }}
      comboboxProps={{ width: 'target' }}
    />
  );
}
