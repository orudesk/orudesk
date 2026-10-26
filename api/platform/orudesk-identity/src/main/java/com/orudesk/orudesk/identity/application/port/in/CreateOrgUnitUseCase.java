package com.orudesk.orudesk.identity.application.port.in;

import java.util.UUID;

public interface CreateOrgUnitUseCase {

    /** @return the new unit's id */
    UUID create(CreateOrgUnitCommand command);
}
