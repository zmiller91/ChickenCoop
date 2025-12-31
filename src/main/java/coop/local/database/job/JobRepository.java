package coop.local.database.job;

import coop.local.database.BaseRepository;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Repository
public class JobRepository extends BaseRepository  {


    public void updateStatus(Job job) {
        sessionFactory.getCurrentSession().createQuery("""
            update Job j
            set j.status = :status,
                j.rank = :rank,
                j.statusUpdateTs = :now
            where j.id = :id
            and j.status <> :status
            and j.rank <= :rank

        """).setParameter("id", job.getId())
            .setParameter("rank", job.getStatus().rank())
            .setParameter("status", job.getStatus())
            .setParameter("now", System.currentTimeMillis())
            .executeUpdate();

        sessionFactory.getCurrentSession().refresh(job);
    }

    public void unreserve(Job job) {
        sessionFactory.getCurrentSession().createQuery("""
            update Job j
            set j.status = :created,
                j.rank = :createdRank,
                j.statusUpdateTs = :now
            where j.id = :id
              and j.status = :reserved
        """)
                .setParameter("id", job.getId())
                .setParameter("created", JobStatus.CREATED)
                .setParameter("createdRank", JobStatus.CREATED.rank())
                .setParameter("reserved", JobStatus.RESERVED)
                .setParameter("now", System.currentTimeMillis())
                .executeUpdate();

        sessionFactory.getCurrentSession().refresh(job);
    }

    public Job findByFrameId(String frameId) {
        return sessionFactory
                .getCurrentSession()
                .createQuery("""
            select j
            from Job j
            join j.downlink dl
            where dl.frameId = :frameId
        """, Job.class)
                .setParameter("frameId", frameId)
                .uniqueResultOptional()
                .orElse(null);
    }


    //TODO: Make the cutoff configuratble?
    public List<Job> findCreatedJobs() {
        long now = System.currentTimeMillis();

        return sessionFactory.getCurrentSession()
                .createQuery("""
            from Job j
            where j.status = :status
              and j.expireAt > :now
            order by j.createdAt asc
        """, Job.class)
                .setParameter("status", JobStatus.CREATED)
                .setParameter("now", now)
                .list();
    }

    //TODO: Make the cutoff configuratble?
    public List<Job> findPendingJobs() {
        long now = System.currentTimeMillis();

        return sessionFactory.getCurrentSession()
                .createQuery("""
            from Job j
            where j.status = :status
              and j.expireAt > :now
            order by j.createdAt asc
        """, Job.class)
                .setParameter("status", JobStatus.PENDING)
                .setParameter("now", now)
                .list();
    }

    // TODO: Make the cutoff configurable?
    public List<Job> findJobsUsingResources() {
        long now = System.currentTimeMillis();

        return sessionFactory.getCurrentSession()
                .createQuery("""
                from Job j
                where j.status in (
                    :pending,
                    :waitingForAck,
                    :waitingForComplete,
                    :reserved
                )
                  and j.expireAt > :now
                order by j.createdAt asc
            """, Job.class)
                .setParameter("pending", JobStatus.PENDING)
                .setParameter("waitingForAck", JobStatus.WAITING_FOR_ACK)
                .setParameter("waitingForComplete", JobStatus.WAITING_FOR_COMPLETE)
                .setParameter("reserved", JobStatus.RESERVED)
                .setParameter("now", now)
                .list();
    }

    public List<Job> findReservedJobsOlderThan(Duration age) {
        long cutoffMillis = System.currentTimeMillis() - age.toMillis();
        return sessionFactory.getCurrentSession()
                .createQuery("""
                from Job j
                where j.status = :status
                  and j.statusUpdateTs < :cutoff
                order by j.createdAt asc
            """, Job.class)
                .setParameter("status", JobStatus.RESERVED)
                .setParameter("cutoff", cutoffMillis)
                .list();
    }

    public Job findReserved(String componentId) {
        return sessionFactory.getCurrentSession()
                .createQuery("""
                from Job j
                where j.componentId = :componentId
                  and j.status = :status
            """, Job.class)
                .setParameter("componentId", componentId)
                .setParameter("status", JobStatus.RESERVED)
                .uniqueResultOptional()
                .orElse(null);
    }

    public Job findWaitingForAck() {
        return sessionFactory.getCurrentSession()
                .createQuery("""
            from Job j
            where j.status = :status
            order by j.createdAt asc
        """, Job.class)
                .setParameter("status", JobStatus.WAITING_FOR_ACK)
                .setMaxResults(1)
                .uniqueResultOptional()
                .orElse(null);
    }

    public int purge() {
        long cutoffMillis = Instant.now().minusSeconds(48L * 60L * 60L).toEpochMilli();

        return sessionFactory.getCurrentSession()
                .createQuery("""
            delete from Job j
            where j.createdAt < :cutoff
        """)
                .setParameter("cutoff", cutoffMillis)
                .executeUpdate();
    }
}
