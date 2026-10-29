import { Alert, Anchor, Button, Group, Loader, Stack, Text, TextInput, Title } from '@mantine/core';
import type { Schemas } from '@orudesk/api-client';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { EditConflictAlert } from '../forms/EditConflictAlert';
import { usePageTitle } from '../layout/usePageTitle';
import { EditConflictError, ValidationError } from '../queries/formErrors';
import { useEditPerson, usePerson } from '../queries/people';

/**
 * Correct a person's details (Day 50). The form is filled from what was read, and the save sends that read's
 * version back: if someone else saved in between, the API refuses (409) and the page offers to reload.
 */
export function EditPersonPage() {
  const { personId = '' } = useParams();
  const person = usePerson(personId);
  const edit = useEditPerson(personId);
  const navigate = useNavigate();
  usePageTitle('Edit person');
  const fieldErrors = edit.error instanceof ValidationError ? edit.error.fields : {};
  const conflict = edit.error instanceof EditConflictError;

  const reload = async () => {
    await person.refetch();
    edit.reset(); // the form is redrawn from the fresh read (its key is the version)
  };

  if (person.isPending) return <Loader size="sm" aria-label="Loading" />;
  if (person.isError) return <Text role="alert">{person.error.message}</Text>;
  if (person.data === null) {
    return (
      <Stack>
        <Title order={1}>Person not found</Title>
        <Text>Nobody with this id is registered here.</Text>
      </Stack>
    );
  }
  const current = person.data;

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? '').trim();
    const optional = (name: string) => text(name) || null; // an empty field means "not given"
    const changed: Schemas['EditPersonRequest'] = {
      version: current.version,
      firstName: text('firstName'),
      lastName: text('lastName'),
      dateOfBirth: optional('dateOfBirth'),
      phone: optional('phone'),
      email: optional('email'),
    };
    edit.mutate(changed, {
      onSuccess: () => void navigate(`/people/${personId}`, { replace: true }),
    });
  };

  return (
    <Stack maw={520}>
      <Anchor component={Link} to={`/people/${personId}`} size="sm">
        ← {current.firstName} {current.lastName}
      </Anchor>
      <Title order={1}>Edit person</Title>
      {conflict && (
        <EditConflictAlert onReload={() => void reload()} reloading={person.isFetching} />
      )}
      {edit.isError && !conflict && !(edit.error instanceof ValidationError) && (
        <Alert color="red" role="alert" title="Not saved">
          {edit.error.message}
        </Alert>
      )}
      {/* Uncontrolled inputs keep their first defaultValue: a new key after a reload redraws them */}
      <form key={current.version} onSubmit={submit} noValidate>
        <Stack>
          <Group grow align="flex-start">
            <TextInput
              name="firstName"
              label="First name"
              withAsterisk
              autoComplete="off"
              defaultValue={current.firstName}
              error={fieldErrors.firstName}
            />
            <TextInput
              name="lastName"
              label="Last name"
              withAsterisk
              autoComplete="off"
              defaultValue={current.lastName}
              error={fieldErrors.lastName}
            />
          </Group>
          <TextInput
            name="dateOfBirth"
            label="Date of birth"
            type="date"
            defaultValue={current.dateOfBirth ?? ''}
            error={fieldErrors.dateOfBirth}
            maw={200}
          />
          <TextInput
            name="phone"
            label="Phone"
            type="tel"
            defaultValue={current.phone ?? ''}
            error={fieldErrors.phone}
          />
          <TextInput
            name="email"
            label="Email"
            type="email"
            defaultValue={current.email ?? ''}
            error={fieldErrors.email}
          />
          <Group>
            <Button type="submit" loading={edit.isPending} disabled={conflict}>
              Save
            </Button>
            <Button component={Link} to={`/people/${personId}`} variant="default">
              Cancel
            </Button>
          </Group>
        </Stack>
      </form>
    </Stack>
  );
}
