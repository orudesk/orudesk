import { coreModule, type OrudeskModule } from '@orudesk/web-core';

/**
 * The standard edition: the core's screens and nothing else. An edition is just this list; the NGO edition's
 * is [coreModule, ngoModule].
 */
export const modules: OrudeskModule[] = [coreModule];
