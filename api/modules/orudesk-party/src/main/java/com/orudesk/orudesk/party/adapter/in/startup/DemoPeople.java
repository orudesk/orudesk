package com.orudesk.orudesk.party.adapter.in.startup;

import com.orudesk.orudesk.party.application.port.in.PeopleSearch;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonCommand;
import com.orudesk.orudesk.party.application.port.in.RegisterPersonUseCase;
import com.orudesk.orudesk.party.application.port.in.SearchPeopleQuery;
import com.orudesk.orudesk.shared.SystemActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A dozen sample people for the demo (the "demo" profile only), into an empty registry only. */
@Component
@Profile("demo")
class DemoPeople {

    private static final Logger log = LoggerFactory.getLogger(DemoPeople.class);

    static final List<RegisterPersonCommand> PEOPLE = List.of(
            person("Asha", "Kumar", "2014-03-09", "+91 98450 11201", null),
            person("Ravi", "Shah", "2013-11-21", "+91 98450 11202", null),
            person("Meera", "Iyer", "1985-06-02", "+91 98450 11203", "meera.iyer@example.org"),
            person("Arjun", "Nair", "2015-01-17", null, null),
            person("Fatima", "Sheikh", "1990-09-30", "+91 98450 11205", "fatima.sheikh@example.org"),
            person("Karthik", "Raman", "1978-04-12", "+91 98450 11206", null),
            person("Priya", "Menon", "2012-08-05", null, null),
            person("John", "Mathew", "1969-12-25", "+91 98450 11208", "john.mathew@example.org"),
            person("Lakshmi", "Pillai", "2016-02-29", null, null),
            person("Sanjay", "Gupta", "1982-07-14", "+91 98450 11210", null),
            person("Anita", "Das", "1995-10-03", "+91 98450 11211", "anita.das@example.org"),
            person("Vikram", "Singh", "2011-05-23", null, null));

    private final RegisterPersonUseCase registerPerson;
    private final SearchPeopleQuery searchPeople;
    private final UUID tenant;

    // The tenant the demo runs in is the identity module's setting; it's configuration, not a code dependency
    DemoPeople(RegisterPersonUseCase registerPerson, SearchPeopleQuery searchPeople,
               @Value("${orudesk.identity.default-tenant-id}") UUID tenant) {
        this.registerPerson = registerPerson;
        this.searchPeople = searchPeople;
        this.tenant = tenant;
    }

    @EventListener(ApplicationReadyEvent.class)
    void load() {
        SystemActor.runAs(tenant, () -> {
            if (!searchPeople.search(new PeopleSearch(null, null, 1)).items().isEmpty()) {
                log.info("The people registry isn't empty, no demo people added");
                return;
            }
            PEOPLE.forEach(registerPerson::register);
            log.info("Demo people loaded: {}", PEOPLE.size());
        });
    }

    private static RegisterPersonCommand person(String first, String last, String born, String phone, String email) {
        return new RegisterPersonCommand(first, last, LocalDate.parse(born), phone, email);
    }
}
