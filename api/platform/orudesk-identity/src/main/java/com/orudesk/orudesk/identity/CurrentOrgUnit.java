package com.orudesk.orudesk.identity;

import java.util.UUID;

/**
 * The org unit a request is for, sent by the client in the X-Org-Unit header (the web app's school switcher).
 * Part of identity's public API. It only says which unit was asked for; whether the user may act there is
 * {@link OrgUnitAccess}'s question.
 */
public interface CurrentOrgUnit {

    String HEADER = "X-Org-Unit";

    /** @return the unit from the header; 400 if it's missing, 404 if the tenant has no such unit */
    UUID require();
}
