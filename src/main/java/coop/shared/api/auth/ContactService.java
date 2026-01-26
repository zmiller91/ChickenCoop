package coop.shared.api.auth;

import coop.shared.database.repository.ContactRepository;
import coop.shared.database.repository.CoopRepository;
import coop.shared.database.table.Contact;
import coop.shared.database.table.Coop;
import coop.shared.exception.NotFound;
import coop.shared.security.AuthContext;
import org.hibernate.sql.Delete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@EnableTransactionManagement
@Transactional
@RestController
@RequestMapping(value = "/contacts")
public class ContactService {

    @Autowired
    private AuthContext userContext;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private ContactRepository contactRepository;

    @GetMapping("{coopId}/list")
    public ListContactsResponse listContacts(@PathVariable("coopId") String coopId) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        return new ListContactsResponse(
                contactRepository.findByCoop(coop).stream().map(ContactService::toDTO).toList()
        );
    }

    @PutMapping("{coopId}/add")
    public AddContactResponse create(@PathVariable("coopId") String coopId,
                                  @RequestBody AddContactRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Contact contact = new Contact();
        contact.setCoop(coop);
        contact.setEmail(request.contact().email());
        contact.setPhone(request.contact().phone());
        contact.setDisplayName(request.contact().displayName());
        contactRepository.persist(contact);

        return new AddContactResponse(toDTO(contact));
    }

    @PutMapping("{coopId}/update")
    public UpdateContactResponse update(@PathVariable String coopId,
                                        @RequestBody UpdateContactRequest request) {
        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Contact contact = contactRepository.findByIdAndCoop(coop, request.contact().id());
        if(contact == null) {
            throw new NotFound("Contact not found.");
        }

        contact.setCoop(coop);
        contact.setEmail(request.contact().email());
        contact.setPhone(request.contact().phone());
        contact.setDisplayName(request.contact().displayName());
        contactRepository.persist(contact);

        return new UpdateContactResponse(toDTO(contact));
    }

    @DeleteMapping("{coopId}/{contactId}")
    public void delete(@PathVariable String coopId, @PathVariable String contactId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if (coop == null) {
            throw new NotFound("Coop not found.");
        }

        Contact contact = contactRepository.findByIdAndCoop(coop, contactId);
        if(contact == null) {
            throw new NotFound("Contact not found.");
        }

        contactRepository.delete(contact);
    }


    public static ContactDTO toDTO(Contact contact) {
        return new ContactDTO(
                contact.getId(),
                contact.getDisplayName(),
                contact.getEmail(),
                contact.getPhone()
        );
    }

    public record ListContactsResponse(List<ContactDTO> contacts){};
    public record ContactDTO(String id, String displayName, String email, String phone){};
    public record AddContactResponse(ContactDTO contact){}
    public record AddContactRequest(ContactDTO contact){}
    public record UpdateContactResponse(ContactDTO contact){}
    public record UpdateContactRequest(ContactDTO contact){}
}
