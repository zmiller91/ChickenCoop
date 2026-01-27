package coop.shared.api.auth;

import coop.shared.database.repository.CoopRepository;
import coop.shared.database.repository.InboxMessageRepository;
import coop.shared.database.table.Coop;
import coop.shared.database.table.inbox.InboxMessage;
import coop.shared.database.table.Severity;
import coop.shared.exception.NotFound;
import coop.shared.security.AuthContext;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Transactional
@RestController
@EnableTransactionManagement
@RequestMapping(value = "/inbox")
public class InboxService {

    private static final int PAGE_SIZE = 20;

    @Autowired
    private AuthContext userContext;

    @Autowired
    private CoopRepository coopRepository;

    @Autowired
    private InboxMessageRepository inboxRepository;

    @GetMapping("/{coopId}/count-new")
    public Long countNew(@PathVariable String coopId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        return inboxRepository.countUnreadByCoop(coop);
    }

    @GetMapping("/{coopId}/list/{page}")
    public ListInboxResponse list(@PathVariable String coopId,
                                  @PathVariable Integer page) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        int pageNumber = ObjectUtils.firstNonNull(page, 0);
        return new ListInboxResponse(
                pageNumber,
                inboxRepository.findByCoop(coop, pageNumber, PAGE_SIZE)
                        .stream().map(InboxService::toDTO).toList()
        );
    }

    @DeleteMapping("/{coopId}/{messageId}")
    public void delete(@PathVariable String coopId, @PathVariable String messageId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        InboxMessage message = inboxRepository.findByCoopAndId(coop, messageId);
        if(message == null) {
            throw new NotFound("Message not found.");
        }

        message.delete();
        inboxRepository.persist(message);
    }

    @PostMapping("/{coopId}/{messageId}/read")
    public void markRead(@PathVariable String coopId, @PathVariable String messageId) {

        Coop coop = coopRepository.findById(userContext.getCurrentUser(), coopId);
        if(coop == null) {
            throw new NotFound("Coop not found.");
        }

        InboxMessage message = inboxRepository.findByCoopAndId(coop, messageId);
        if(message == null) {
            throw new NotFound("Message not found.");
        }

        message.markRead();
        inboxRepository.persist(message);
    }

    public static InboxMessageDTO toDTO(InboxMessage message) {
        return new InboxMessageDTO(
                message.getId(),
                message.getSeverity(),
                message.getCreatedTs(),
                message.getReadTs(),
                message.getArchivedTs(),
                message.getSubject(),
                message.getBodyText(),
                message.getBodyHtml()
        );
    }

    public record ListInboxResponse(int page, List<InboxMessageDTO> messages){}
    public record InboxMessageDTO(String id,
                                  Severity severity,
                                  Instant createdTs,
                                  Instant readTs,
                                  Instant archivedTs,
                                  String subject,
                                  String bodyText,
                                  String bodyHtml){}

}
