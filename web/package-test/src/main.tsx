// What an edition's main.tsx looks like with the published packages.
import '@mantine/core/styles.css';
import '@orudesk/web-core/styles.css';
import { coreModule, startOrudeskApp } from '@orudesk/web-core';

startOrudeskApp([coreModule]);
