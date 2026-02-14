package coop.local.database.downlink;

import coop.local.database.BaseRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(transactionManager = "piTransactionManager")
public class DownlinkRepository extends BaseRepository {
}
