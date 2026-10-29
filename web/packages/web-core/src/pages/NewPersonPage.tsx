import { Alert, Button, Group, Stack, TextInput, Title } from '@mantine/core';
import type { Schemas } from '@orudesk/api-client';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { usePageTitle } from '../layout/usePageTitle';
import { ValidationError } from '../queries/formErrors';
import { useRegisterPerson } from '../queries/people';

/**
 * Add a person. The server checks the input (it has to anyway: other clients exist), and its message for each
 * field appears under that field. The browser only marks what's required.
 */
export function NewPersonPage() {
  usePageTitle('Add person');
  const register = useRegisterPerson();
  const navigate = useNavigate();
  const fieldErrors = register.error instanceof ValidationError ? register.error.fields : {};

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? '').trim();
    const optional = (name: string) => text(name) || null; // an empty field means "not given"
    const person: Schemas['RegisterPersonRequest'] = {
      firstName: text('firstName'),
      lastName: text('lastName'),
      dateOfBirth: optional('dateOfBirth'),
      phone: optional('phone'),
      email: optional('email'),
    };
    register.mutate(person, {
      onSuccess: (id) => void navigate(`/people/${id}`, { replace: true }),
    });
  };

  return (
    <Stack maw={520}>
      <Title order={1}>Add person</Title>
      {register.isError && !(register.error instanceof ValidationError) && (
        <Alert color="red" role="alert" title="Not saved">
          {register.error.message}
        </Alert>
      )}
      <form onSubmit={submit} noValidate>
        <Stack>
          <Group grow align="flex-start">
            <TextInput
              name="firstName"
              label="First name"
              withAsterisk
              autoComplete="off"
              error={fieldErrors.firstName}
            />
            <TextInput
              name="lastName"
              label="Last name"
              withAsterisk
              autoComplete="off"
              error={fieldErrors.lastName}
            />
          </Group>
          <TextInput
            name="dateOfBirth"
            label="Date of birth"
            type="date"
            error={fieldErrors.dateOfBirth}
            maw={200}
          />
          <TextInput name="phone" label="Phone" type="tel" error={fieldErrors.phone} />
          <TextInput name="email" label="Email" type="email" error={fieldErrors.email} />
          <Group>
            <Button type="submit" loading={register.isPending}>
              Save
            </Button>
            <Button component={Link} to="/people" variant="default">
              Cancel
            </Button>
          </Group>
        </Stack>
      </form>
    </Stack>
  );
}
